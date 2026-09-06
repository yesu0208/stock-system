package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OtocoService {

    private final OtocoRepository otocoRepository;
    private final OtocoEntryBookRegistry otocoEntryBookRegistry;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final AccountApiClient accountApiClient;

    public void registerOtoco(StockServerOtocoRequestEvent request) {

        LeverageRatio leverageRatio = request.leverageRatio() == null ? LeverageRatio.SPOT : request.leverageRatio();

        Integer tpTriggerPrice = OtocoPriceResolver.resolveTakeProfit(
                request.entryTriggerPrice(), request.tpMode(), request.tpPrice(), request.tpPct());
        Integer slTriggerPrice = OtocoPriceResolver.resolveStopLoss(
                request.entryTriggerPrice(), request.slMode(), request.slPrice(), request.slPct());

        long orderAmount = (long) request.entryTriggerPrice() * request.orderQuantity();
        long reserveAmount = leverageRatio.isSpot() ? orderAmount : leverageRatio.calculateMarginDeposit(orderAmount);

        boolean reserved = accountApiClient.reserveCash(request.username(), reserveAmount);
        if (!reserved) {
            otocoResponseEventPublisher.publishError(request, OtocoResultCode.INSUFFICIENT_BALANCE);
            return;
        }

        OtocoEntity savedOtoco;

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
            accountApiClient.refundReservedCash(request.username(), reserveAmount);
            otocoResponseEventPublisher.publishError(request, OtocoResultCode.INTERNAL_ERROR);
            throw e;
        }

        var responseMessage = StockServerOtocoResponseMessage.fromEntity(savedOtoco);
        stockServerOtocoResponseRepository.save(responseMessage);
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
