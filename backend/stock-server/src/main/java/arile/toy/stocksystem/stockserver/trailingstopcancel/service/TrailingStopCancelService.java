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
                    // 예외를 삼키면 CANCELED 상태만 커밋되고 예약분은 환불되지 않은 채 남음
                    // → 다시 던져 트랜잭션을 롤백하고 컨슈머 재시도로 처리
                    throw e;
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

        // 롤백 가능한 DB 작업을 먼저 확정: 환불 이후 저장이 실패해 롤백되면 ACTIVE로 돌아가
        // 예약금 없는 트레일링 스탑이 북에 남고, 재시도 시 이중 환불될 수 있음
        trailingStopCancelRepository.saveAndFlush(TrailingStopCancelEntity.of(entity.getTrailingStopId()));

        // 되돌릴 수 없는 외부 환불
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

        // 메모리 북에서 제거: 환불 이후이므로 실패해도 예외를 던지지 않음 (롤백·재시도 시 이중 환불 방지)
        // 북에 남더라도 발동 시 상태가 CANCELED라 주문되지 않음
        try {
            trailingStopBookRegistry.remove(entity.getStockCode(), entity.getTrailingStopId());
        } catch (Exception e) {
            log.warn("Trailing stop book remove failed after cancel. trailingStopId={}", entity.getTrailingStopId(), e);
        }
    }

    private void publishSuccess(TrailingStopEntity entity) {

        TrailingStopCancelResponseEvent event = TrailingStopCancelResponseEvent.of(entity, true, null);

        // 취소는 이미 완료됨: 응답 캐시 삭제 실패가 예외로 전파되면 롤백·재시도로 이중 환불될 수 있으므로 로그만 남김
        try {
            stockServerTrailingStopResponseRepository.delete(event.username(), event.trailingStopId());
        } catch (Exception e) {
            log.warn("Trailing stop response delete failed after cancel. trailingStopId={}", event.trailingStopId(), e);
        }

        trailingStopCancelResponseEventPublisher.publish(event);
    }
}
