package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoService {

    private final OtocoRepository otocoRepository;
    private final OtocoEntryBookRegistry otocoEntryBookRegistry;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final AccountApiClient accountApiClient;
    private final ReserveAmountCalculator reserveAmountCalculator;

    public void registerOtoco(StockServerOtocoRequestEvent request) {

        LeverageRatio leverageRatio = request.leverageRatio() == null ? LeverageRatio.SPOT : request.leverageRatio();

        Integer tpTriggerPrice = OtocoPriceResolver.resolveTakeProfit(
                request.entryTriggerPrice(), request.tpMode(), request.tpPrice(), request.tpPct());
        Integer slTriggerPrice = OtocoPriceResolver.resolveStopLoss(
                request.entryTriggerPrice(), request.slMode(), request.slPrice(), request.slPct());

        if (tpTriggerPrice <= request.entryTriggerPrice()) {
            otocoResponseEventPublisher.publishError(request, OtocoResultCode.INVALID_TP_PRICE);
            return;
        }
        if (slTriggerPrice >= request.entryTriggerPrice()) {
            otocoResponseEventPublisher.publishError(request, OtocoResultCode.INVALID_SL_PRICE);
            return;
        }

        long orderAmount = (long) request.entryTriggerPrice() * request.orderQuantity();
        long reserveAmount = reserveAmountCalculator.calculateReserveAmount(leverageRatio, orderAmount);

        boolean reserved = accountApiClient.reserveCash(request.username(), reserveAmount);
        if (!reserved) {
            otocoResponseEventPublisher.publishError(request, OtocoResultCode.INSUFFICIENT_BALANCE);
            return;
        }

        OtocoEntity savedOtoco = null;

        try {
            OtocoEntity entity = OtocoEntity.of(
                    request.username(), request.stockCode(), request.entryDirection(), leverageRatio,
                    request.orderQuantity(), request.entryTriggerPrice(),
                    request.tpMode(), request.tpPrice(), request.tpPct(), tpTriggerPrice,
                    request.slMode(), request.slPrice(), request.slPct(), slTriggerPrice
            );
            savedOtoco = otocoRepository.save(entity);

            otocoEntryBookRegistry.register(OtocoDto.fromEntity(savedOtoco));

        } catch (Exception e) {
            // 저장 이후 실패 시 예약분만 환불하고 WAITING_ENTRY로 남겨 두면, 재시작 시 워밍업으로
            // 예약금 없는 OTOCO가 북에 복구될 수 있음 → 북에서 제거하고 CANCELED로 저장해 무효화
            if (savedOtoco != null) {
                try {
                    otocoEntryBookRegistry.remove(savedOtoco.getStockCode(), savedOtoco.getOtocoId());
                    savedOtoco.changeStatus(OtocoStatus.CANCELED);
                    otocoRepository.save(savedOtoco);
                } catch (Exception cancelException) {
                    e.addSuppressed(cancelException);
                }
            }

            accountApiClient.refundReservedCash(request.username(), reserveAmount);
            otocoResponseEventPublisher.publishError(request, OtocoResultCode.INTERNAL_ERROR);
            throw e;
        }

        var responseMessage = StockServerOtocoResponseMessage.fromEntity(savedOtoco);

        // 등록은 이미 완료됨: 응답 캐시 저장 실패가 예외로 전파되면 컨슈머 재시도로 예약·등록이 중복되므로 로그만 남김
        try {
            stockServerOtocoResponseRepository.save(responseMessage);
        } catch (Exception e) {
            log.warn("Otoco response save failed after registration. otocoId={}", savedOtoco.getOtocoId(), e);
        }
        otocoResponseEventPublisher.publish(responseMessage);
    }

    @Transactional
    public java.util.Optional<OtocoEntity> findByIdForUpdate(Long otocoId) {
        return otocoRepository.findByIdForUpdate(otocoId);
    }

    @org.springframework.transaction.annotation.Transactional
    public List<OtocoEntity> findAllUnfinishedOtocos(List<String> stockCodes) {
        return otocoRepository.findAllUnfinished(stockCodes);
    }
}
