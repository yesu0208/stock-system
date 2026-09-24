package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.trailingstop.dto.*;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.event.StockServerTrailingStopRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstop.event.publisher.TrailingStopResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
import arile.toy.stocksystem.stockserver.trailingstop.repository.StockServerTrailingStopResponseRepository;
import arile.toy.stocksystem.stockserver.trailingstop.repository.TrailingStopRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrailingStopService {

    private final TrailingStopRepository trailingStopRepository;
    private final TrailingStopBookRegistry trailingStopBookRegistry;
    private final TrailingStopResponseEventPublisher trailingStopResponseEventPublisher;
    private final StockServerTrailingStopResponseRepository stockServerTrailingStopResponseRepository;
    private final AccountApiClient accountApiClient;
    private final ReserveAmountCalculator reserveAmountCalculator;

    public void registerTrailingStop(StockServerTrailingStopRequestEvent request) {

        LeverageRatio leverageRatio = request.leverageRatio() == null ? LeverageRatio.SPOT : request.leverageRatio();

        int initialTriggerPrice = TrailingStopPriceCalculator.calcTrigger(
                request.trailingStopType(), request.basePrice(), request.stopPercent());

        long reserveAmount = 0L;

        if (request.trailingStopType() == TrailingStopType.BUY) {

            long orderAmount = (long) initialTriggerPrice * request.orderQuantity();
            reserveAmount = reserveAmountCalculator.calculateReserveAmount(leverageRatio, orderAmount);

            boolean reserved = accountApiClient.reserveCash(request.username(), reserveAmount);
            if (!reserved) {
                trailingStopResponseEventPublisher.publishError(request, TrailingStopResultCode.INSUFFICIENT_BALANCE);
                return;
            }

        } else {
            boolean reserved = leverageRatio.isSpot()
                    ? accountApiClient.reserveStock(request.username(), request.stockCode(), request.orderQuantity())
                    : accountApiClient.reserveLeverageStock(request.username(), request.stockCode(), leverageRatio.name(), request.orderQuantity());

            if (!reserved) {
                trailingStopResponseEventPublisher.publishError(request, TrailingStopResultCode.INSUFFICIENT_STOCK);
                return;
            }
        }

        TrailingStopEntity savedTrailingStop;

        try {
            TrailingStopEntity entity = TrailingStopEntity.of(
                    request.username(),
                    request.stockCode(),
                    request.trailingStopType(),
                    leverageRatio,
                    request.orderQuantity(),
                    request.stopPercent(),
                    request.basePrice(),
                    initialTriggerPrice,
                    TrailingStopStatus.ACTIVE
            );
            savedTrailingStop = trailingStopRepository.save(entity);

            var dto = TrailingStopDto.fromEntity(savedTrailingStop);
            trailingStopBookRegistry.register(dto);

        } catch (Exception e) {
            if (request.trailingStopType() == TrailingStopType.BUY) {
                accountApiClient.refundReservedCash(request.username(), reserveAmount);
            } else {
                if (leverageRatio.isSpot()) {
                    accountApiClient.refundReservedStock(request.username(), request.stockCode(), request.orderQuantity());
                } else {
                    accountApiClient.refundReservedLeverageStock(
                            request.username(), request.stockCode(), leverageRatio.name(), request.orderQuantity());
                }
            }
            trailingStopResponseEventPublisher.publishError(request, TrailingStopResultCode.INTERNAL_ERROR);
            throw e;
        }

        var responseMessage = StockServerTrailingStopResponseMessage.fromDto(TrailingStopDto.fromEntity(savedTrailingStop));

        stockServerTrailingStopResponseRepository.save(responseMessage);
        trailingStopResponseEventPublisher.publish(responseMessage);
    }

    @Transactional
    public UpdateTrailingStopStatusResult updateTrailingStopStatusByTrigger(Long trailingStopId) {

        TrailingStopEntity entity = trailingStopRepository.findByIdForUpdate(trailingStopId)
                .orElseThrow(() -> new IllegalArgumentException("trailing stop not found"));

        TrailingStopStatus prevStatus = entity.getTrailingStopStatus();

        if (prevStatus != TrailingStopStatus.ACTIVE) {
            return UpdateTrailingStopStatusResult.of(entity, prevStatus);
        }

        entity.changeTrailingStopStatus(TrailingStopStatus.TRIGGERED);
        return UpdateTrailingStopStatusResult.of(entity, prevStatus);
    }

    /** 시스템 강제 취소(장 마감 정리 등)용. 소유자 검증 없이 취소 상태로 변경. */
    @Transactional
    public UpdateTrailingStopStatusResult updateTrailingStopStatusByCancel(Long trailingStopId) {

        TrailingStopEntity entity = trailingStopRepository.findByIdForUpdate(trailingStopId)
                .orElseThrow(() -> new IllegalArgumentException("trailing stop not found"));

        return markCanceled(entity);
    }

    /**
     * 사용자 취소 요청용.
     * 요청자가 트레일링 스탑 소유자이고 요청 종목코드가 트레일링 스탑 종목코드와 같을 때만 취소 상태로 변경함.
     * 불일치 시 상태를 변경하지 않고 empty를 반환. (타인 트레일링 스탑 취소, 다른 샤드로의 취소 라우팅 방지)
     */
    @Transactional
    public Optional<UpdateTrailingStopStatusResult> updateTrailingStopStatusByUserCancel(
            Long trailingStopId, String username, String stockCode) {

        TrailingStopEntity entity = trailingStopRepository.findByIdForUpdate(trailingStopId)
                .orElseThrow(() -> new IllegalArgumentException("trailing stop not found"));

        if (username == null
                || !username.equals(entity.getUsername())
                || !entity.getStockCode().equals(stockCode)) {
            return Optional.empty();
        }

        return Optional.of(markCanceled(entity));
    }

    private UpdateTrailingStopStatusResult markCanceled(TrailingStopEntity entity) {

        TrailingStopStatus prevStatus = entity.getTrailingStopStatus();

        if (prevStatus == TrailingStopStatus.CANCELED || prevStatus == TrailingStopStatus.TRIGGERED) {
            return UpdateTrailingStopStatusResult.of(entity, prevStatus);
        }

        entity.changeTrailingStopStatus(TrailingStopStatus.CANCELED);
        return UpdateTrailingStopStatusResult.of(entity, prevStatus);
    }

    @Transactional
    public List<TrailingStopEntity> findAllUntriggeredTrailingStops(List<String> stockCodes) {
        return trailingStopRepository.findAllUntriggered(stockCodes);
    }
}
