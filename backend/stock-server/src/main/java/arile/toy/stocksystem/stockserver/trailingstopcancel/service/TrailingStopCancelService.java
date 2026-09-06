package arile.toy.stocksystem.stockserver.trailingstopcancel.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.dto.UpdateTrailingStopStatusResult;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
import arile.toy.stocksystem.stockserver.trailingstop.repository.StockServerTrailingStopResponseRepository;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopService;
import arile.toy.stocksystem.stockserver.trailingstopcancel.dto.TrailingStopCancelErrorCode;
import arile.toy.stocksystem.stockserver.trailingstopcancel.entity.TrailingStopCancelEntity;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.publisher.TrailingStopCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trailingstopcancel.repository.TrailingStopCancelRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrailingStopCancelService {

    private final TrailingStopService trailingStopService;
    private final TrailingStopCancelRepository trailingStopCancelRepository;
    private final TrailingStopBookRegistry trailingStopBookRegistry;
    private final TrailingStopCancelResponseEventPublisher trailingStopCancelResponseEventPublisher;
    private final StockServerTrailingStopResponseRepository stockServerTrailingStopResponseRepository;
    private final AccountApiClient accountApiClient;

    @Transactional
    public void registerCancel(TrailingStopCancelRequestEvent request) {

        UpdateTrailingStopStatusResult result = trailingStopService.updateTrailingStopStatusByCancel(request.trailingStopId());
        var entity = result.trailingStopEntity();

        switch (result.previousStatus()) {

            case CANCELED -> trailingStopCancelResponseEventPublisher.publish(
                    TrailingStopCancelResponseEvent.of(entity, false, TrailingStopCancelErrorCode.ALREADY_CANCELLED));

            case TRIGGERED -> trailingStopCancelResponseEventPublisher.publish(
                    TrailingStopCancelResponseEvent.of(entity, false, TrailingStopCancelErrorCode.ALREADY_TRIGGERED));

            default -> {
                try {
                    cancelInternal(entity);
                    publishSuccess(entity);
                } catch (Exception e) {
                    log.error("Trailing stop cancel failed. trailingStopId={}", entity.getTrailingStopId(), e);
                    trailingStopCancelResponseEventPublisher.publish(
                            TrailingStopCancelResponseEvent.of(entity, false, TrailingStopCancelErrorCode.INTERNAL_ERROR));
                }
            }
        }
    }

    @Transactional
    public void forceCancelTrailingStop(Long trailingStopId) {

        UpdateTrailingStopStatusResult result = trailingStopService.updateTrailingStopStatusByCancel(trailingStopId);

        if (!result.previousStatus().isOpen()) {
            return;
        }

        var entity = result.trailingStopEntity();

        try {
            cancelInternal(entity);
            publishSuccess(entity);
        } catch (Exception e) {
            log.error("Force trailing stop cancel failed. trailingStopId={}", trailingStopId, e);
        }
    }

    private void cancelInternal(TrailingStopEntity entity) {

        boolean refunded;
        LeverageRatio leverageRatio = entity.getLeverageRatio();

        if (entity.getTrailingStopType() == TrailingStopType.BUY) {

            // 등록 시 예약한 금액과 정확히 일치시켜야 하므로 최초 발동가(triggerPrice, 등록 이후 불변)를 사용함.
            long reservedAmount = (long) entity.getTriggerPrice() * entity.getOrderQuantity();
            long refundAmount = leverageRatio.isSpot() ? reservedAmount : leverageRatio.calculateMarginDeposit(reservedAmount);

            refunded = accountApiClient.refundReservedCash(entity.getUsername(), refundAmount);

            if (!refunded) {
                log.error("Redis cash refund failed. trailingStopId={}, username={}",
                        entity.getTrailingStopId(), entity.getUsername());
                throw new IllegalStateException("Cash refund failed");
            }

        } else {

            refunded = leverageRatio.isSpot()
                    ? accountApiClient.refundReservedStock(entity.getUsername(), entity.getStockCode(), entity.getOrderQuantity())
                    : accountApiClient.refundReservedLeverageStock(entity.getUsername(), entity.getStockCode(),
                    leverageRatio.name(), entity.getOrderQuantity());

            if (!refunded) {
                log.error("Redis stock refund failed. trailingStopId={}, username={}",
                        entity.getTrailingStopId(), entity.getUsername());
                throw new IllegalStateException("Stock refund failed");
            }
        }

        trailingStopCancelRepository.save(TrailingStopCancelEntity.of(entity.getTrailingStopId()));

        trailingStopBookRegistry.remove(entity.getStockCode(), entity.getTrailingStopId());
    }

    private void publishSuccess(TrailingStopEntity entity) {

        TrailingStopCancelResponseEvent event = TrailingStopCancelResponseEvent.of(entity, true, null);

        stockServerTrailingStopResponseRepository.delete(event.username(), event.trailingStopId());

        trailingStopCancelResponseEventPublisher.publish(event);
    }
}
