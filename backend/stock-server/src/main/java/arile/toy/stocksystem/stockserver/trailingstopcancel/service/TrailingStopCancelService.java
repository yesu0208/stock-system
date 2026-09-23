package arile.toy.stocksystem.stockserver.trailingstopcancel.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
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
    private final ReserveAmountCalculator reserveAmountCalculator;

    @Transactional
    public void registerCancel(TrailingStopCancelRequestEvent request) {

        // 요청자가 트레일링 스탑 소유자가 아니거나 종목코드가 다르면 취소하지 않음 (타인 트레일링 스탑 취소 방지)
        // 취소 응답은 소유자 채널로 발행되므로, 거부 시에는 응답을 보내지 않고 로그만 남김.
        var ownedResult = trailingStopService.updateTrailingStopStatusByUserCancel(
                request.trailingStopId(), request.username(), request.stockCode());

        if (ownedResult.isEmpty()) {
            log.warn("Trailing stop cancel rejected: not the owner or stock code mismatch. trailingStopId={}, requester={}, stockCode={}",
                    request.trailingStopId(), request.username(), request.stockCode());
            return;
        }

        UpdateTrailingStopStatusResult result = ownedResult.get();
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
            long refundAmount = reserveAmountCalculator.calculateReserveAmount(leverageRatio, reservedAmount);

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
