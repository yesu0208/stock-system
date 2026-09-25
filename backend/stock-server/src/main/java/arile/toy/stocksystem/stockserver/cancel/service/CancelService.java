package arile.toy.stocksystem.stockserver.cancel.service;

import arile.toy.stocksystem.stockserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.stockserver.cancel.entity.CancelEntity;
import arile.toy.stocksystem.stockserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.cancel.event.publisher.CancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.cancel.repository.CancelRepository;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.dto.UpdateOrderStatusResult;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.QueuePositionBroadcastService;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoOrderLifecycleListener;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelService {

    private final OrderService orderService;
    private final CancelRepository cancelRepository;
    private final OrderQueueRegistry orderQueueRegistry;
    private final CancelResponseEventPublisher cancelResponseEventPublisher;
    private final StockServerOrderResponseRepository stockServerOrderResponseRepository;
    private final AccountApiClient accountApiClient;
    private final OtocoOrderLifecycleListener otocoOrderLifecycleListener;
    private final QueuePositionBroadcastService queuePositionBroadcastService;

    @Transactional
    public void registerCancel(CancelRequestEvent request) {

        // 요청자가 주문 소유자가 아니거나 종목코드가 다르면 취소하지 않음 (타인 주문 취소 방지)
        // 취소 응답은 주문 소유자 채널로 발행되므로, 거부 시에는 응답을 보내지 않고 로그만 남김
        var ownedResult = orderService.updateOrderStatusByUserCancel(
                request.orderId(), request.username(), request.stockCode());

        if (ownedResult.isEmpty()) {
            log.warn("Cancel rejected: not the order owner or stock code mismatch. orderId={}, requester={}, stockCode={}",
                    request.orderId(), request.username(), request.stockCode());
            return;
        }

        UpdateOrderStatusResult result = ownedResult.get();

        var orderEntity = result.orderEntity();

        switch (result.previousStatus()) {

            case CANCELED -> cancelResponseEventPublisher.publish(
                    CancelResponseEvent.of(orderEntity, false, CancelErrorCode.ALREADY_CANCELLED));

            case FILLED -> cancelResponseEventPublisher.publish(
                    CancelResponseEvent.of(orderEntity, false, CancelErrorCode.ALREADY_FILLED));

            default -> {

                try {
                    cancelInternal(orderEntity);
                    publishSuccess(orderEntity);

                } catch (Exception e) {

                    log.error("Cancel failed. orderId={}",
                            orderEntity.getOrderId(), e);

                    cancelResponseEventPublisher.publish(
                            CancelResponseEvent.of(orderEntity, false, CancelErrorCode.INTERNAL_ERROR));
                    throw e;
                }
            }
        }
    }

    @Transactional
    public void forceCancel(Long orderId) {

        UpdateOrderStatusResult result = orderService.updateOrderStatusByCancelEvent(orderId);

        if (!result.previousStatus().isOpen()) {
            return;
        }

        var orderEntity = result.orderEntity();

        try {
            cancelInternal(orderEntity);
            publishSuccess(orderEntity);
        } catch (Exception e) {
            log.error("Force cancel failed. orderId={}", orderId, e);
        }
    }

    /**
     * 실행 순서 주의: 되돌릴 수 있는 DB 작업 -> 되돌릴 수 없는 외부 환불 -> 메모리 대기열 제거 -> 부가 알림.
     * 환불 이후 단계에서 예외가 나 트랜잭션이 롤백되면, 주문은 OPEN으로 돌아가는데 돈은 이미 환불된 상태가 되고
     * 재시도 시 한 번 더 환불됨. 따라서 실패할 수 있는 DB 작업은 모두 환불 전에 끝내고,
     * 환불 이후에는 예외가 밖으로 나가지 않도록 함.
     */
    private void cancelInternal(OrderEntity orderEntity) {

        cancelRepository.save(
                CancelEntity.of(orderEntity.getOrderId())
        );

        otocoOrderLifecycleListener.onOrderCanceled(orderEntity.getOrderId());

        refund(orderEntity);

        // 환불까지 끝난 뒤에 대기열에서 제거 (환불 실패로 롤백되면 주문은 대기열에 그대로 남아 있어야 함)
        orderQueueRegistry.orderCancel(
                orderEntity.getOrderId(),
                orderEntity.getStockCode()
        );

        // 대기열 순번 알림은 부가 기능: 실패해도 취소 결과(환불 완료)를 되돌리지 않음
        try {
            queuePositionBroadcastService.broadcast(orderEntity.getStockCode(), orderEntity.getOrderType());
        } catch (Exception e) {
            log.warn("Queue position broadcast failed after cancel. orderId={}", orderEntity.getOrderId(), e);
        }
    }

    private void refund(OrderEntity orderEntity) {

        boolean refunded;
        LeverageRatio leverageRatio = orderEntity.getLeverageRatio();

        if (orderEntity.getOrderType() == OrderType.BUY) {

            long orderAmount = (long) orderEntity.getOrderPrice() * orderEntity.getRemainingQuantity();

            // 원금(증거금)도 fee와 동일하게 재계산(calculateMarginDeposit) 대신
            // OrderEntity에 정확히 남아있는 remainingReservedMargin을 그대로 환불.
            // 현금(SPOT) 주문은 애초에 증거금 개념이 없어 orderAmount 그대로 사용.
            long principal = leverageRatio.isSpot()
                    ? orderAmount
                    : (orderEntity.getRemainingReservedMargin() != null
                    ? orderEntity.getRemainingReservedMargin()
                    : leverageRatio.calculateMarginDeposit(orderAmount));

            long remainingFee = orderEntity.getRemainingReservedFee() != null
                    ? orderEntity.getRemainingReservedFee()
                    : 0L;

            long refundAmount = principal + remainingFee;

            refunded = accountApiClient.refundReservedCash(orderEntity.getUsername(), refundAmount);

            if (!refunded) {
                log.error("Redis cash refund failed. orderId={}, username={}",
                        orderEntity.getOrderId(),
                        orderEntity.getUsername());

                throw new IllegalStateException("Cash refund failed");
            }

        } else {

            refunded = leverageRatio.isSpot()
                    ? accountApiClient.refundReservedStock(orderEntity.getUsername(), orderEntity.getStockCode(), orderEntity.getRemainingQuantity())
                    : accountApiClient.refundReservedLeverageStock(orderEntity.getUsername(), orderEntity.getStockCode(),
                    leverageRatio.name(), orderEntity.getRemainingQuantity());

            if (!refunded) {
                log.error("Redis stock refund failed. orderId={}, username={}",
                        orderEntity.getOrderId(),
                        orderEntity.getUsername());

                throw new IllegalStateException("Stock refund failed");
            }
        }
    }

    private void publishSuccess(OrderEntity orderEntity) {

        CancelResponseEvent event = CancelResponseEvent.of(orderEntity, true, null);

        // 환불 이후 단계이므로 Redis 응답 삭제 실패로 트랜잭션이 롤백되지 않도록 예외를 삼킴
        try {
            stockServerOrderResponseRepository.delete(event.username(), event.orderId());
        } catch (Exception e) {
            log.warn("Order response delete failed after cancel. orderId={}", event.orderId(), e);
        }

        cancelResponseEventPublisher.publish(event);
    }
}
