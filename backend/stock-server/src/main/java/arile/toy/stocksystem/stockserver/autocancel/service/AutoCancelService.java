package arile.toy.stocksystem.stockserver.autocancel.service;

import arile.toy.stocksystem.stockserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.autocancel.entity.AutoCancelEntity;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.publisher.AutoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autocancel.repository.AutoCancelRepository;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.UpdateAutoOrderStatusResult;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCancelService {

    private final AutoOrderService autoOrderService;
    private final AutoCancelRepository autoCancelRepository;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoCancelResponseEventPublisher autoCancelResponseEventPublisher;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final AccountApiClient accountApiClient;
    private final ReserveAmountCalculator reserveAmountCalculator;

    @Transactional
    public void registerAutoCancel(AutoCancelRequestEvent request) {

        // 요청자가 자동 주문 소유자가 아니거나 종목코드가 다르면 취소하지 않음 (타인 자동 주문 취소 방지)
        // 취소 응답은 자동 주문 소유자 채널로 발행되므로, 거부 시에는 응답을 보내지 않고 로그만 남김
        var ownedResult = autoOrderService.updateAutoOrderStatusByUserCancel(
                request.autoOrderId(), request.username(), request.stockCode());

        if (ownedResult.isEmpty()) {
            log.warn("Auto cancel rejected: not the auto order owner or stock code mismatch. autoOrderId={}, requester={}, stockCode={}",
                    request.autoOrderId(), request.username(), request.stockCode());
            return;
        }

        UpdateAutoOrderStatusResult result = ownedResult.get();
        var autoOrderEntity = result.autoOrderEntity();

        switch (result.previousStatus()) {
            case CANCELED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.ALREADY_CANCELLED));

            case TRIGGERED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity,false, AutoCancelErrorCode.ALREADY_TRIGGERED));

            default -> {

                try {
                    cancelInternal(autoOrderEntity);
                    publishSuccess(autoOrderEntity);

                } catch (Exception e) {

                    log.error("Auto cancel failed. autoOrderId={}",
                            autoOrderEntity.getAutoOrderId(), e);

                    autoCancelResponseEventPublisher.publish(
                            AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.INTERNAL_ERROR));

                    // 예외를 삼키면 트랜잭션이 커밋되어, 환불되지 않은 자동주문이 CANCELED로 남고 재시도도 불가능해짐
                    // → 다시 던져 롤백(ACTIVE 유지)하고 컨슈머 재시도로 넘김
                    throw e;
                }
            }
        }
    }

    @Transactional
    public void forceAutoCancel(Long autoOrderId) {

        UpdateAutoOrderStatusResult result = autoOrderService.updateAutoOrderStatusByCancel(autoOrderId);

        if (!result.previousStatus().isOpen()) {
            return;
        }

        var autoOrderEntity = result.autoOrderEntity();

        try {
            cancelInternal(autoOrderEntity);
            publishSuccess(autoOrderEntity);
        } catch (Exception e) {
            log.error("Force auto cancel failed. autoOrderId={}", autoOrderId, e);
        }
    }

    /**
     * 실행 순서 주의: 되돌릴 수 있는 DB 작업 -> 되돌릴 수 없는 외부 환불 -> 메모리 대기열 제거.
     * 환불 이후 단계에서 예외가 나 트랜잭션이 롤백되면, 자동주문은 ACTIVE로 돌아가는데 돈은 이미 환불된 상태가 되고
     * 재시도 시 한 번 더 환불됨. 따라서 실패할 수 있는 DB 작업은 환불 전에 끝냄.
     */
    private void cancelInternal(AutoOrderEntity autoOrderEntity) {

        autoCancelRepository.save(
                AutoCancelEntity.of(autoOrderEntity.getAutoOrderId())
        );

        refund(autoOrderEntity);

        // 환불까지 끝난 뒤에 대기열에서 제거 (환불 실패로 롤백되면 자동주문은 대기열에 그대로 남아 있어야 함)
        autoOrderQueueRegistry.autoOrderCancel(
                autoOrderEntity.getAutoOrderId(),
                autoOrderEntity.getStockCode()
        );
    }

    private void refund(AutoOrderEntity autoOrderEntity) {

        boolean refunded;
        LeverageRatio leverageRatio = autoOrderEntity.getLeverageRatio();

        if (autoOrderEntity.getAutoOrderType() == AutoOrderType.BUY) {

            long orderAmount = (long) autoOrderEntity.getOrderPrice() * autoOrderEntity.getOrderQuantity();
            long refundAmount = reserveAmountCalculator.calculateReserveAmount(leverageRatio, orderAmount);

            refunded = accountApiClient.refundReservedCash(autoOrderEntity.getUsername(), refundAmount);

            if (!refunded) {
                log.error("Redis cash refund failed. autoOrderId={}, username={}",
                        autoOrderEntity.getAutoOrderId(),
                        autoOrderEntity.getUsername());

                throw new IllegalStateException("Cash refund failed");
            }

        } else {

            refunded = leverageRatio.isSpot()
                    ? accountApiClient.refundReservedStock(autoOrderEntity.getUsername(), autoOrderEntity.getStockCode(), autoOrderEntity.getOrderQuantity())
                    : accountApiClient.refundReservedLeverageStock(autoOrderEntity.getUsername(), autoOrderEntity.getStockCode(),
                    leverageRatio.name(), autoOrderEntity.getOrderQuantity());

            if (!refunded) {
                log.error("Redis stock refund failed. autoOrderId={}, username={}",
                        autoOrderEntity.getAutoOrderId(),
                        autoOrderEntity.getUsername());

                throw new IllegalStateException("Stock refund failed");
            }
        }
    }

    private void publishSuccess(AutoOrderEntity autoOrderEntity) {

        AutoCancelResponseEvent event = AutoCancelResponseEvent.of(autoOrderEntity, true, null);

        // 환불 이후 단계이므로 Redis 응답 삭제 실패로 트랜잭션이 롤백되지 않도록 예외를 삼킴
        try {
            stockServerAutoOrderResponseRepository.delete(event.username(), event.autoOrderId());
        } catch (Exception e) {
            log.warn("Auto order response delete failed after cancel. autoOrderId={}", event.autoOrderId(), e);
        }

        autoCancelResponseEventPublisher.publish(event);
    }
}
