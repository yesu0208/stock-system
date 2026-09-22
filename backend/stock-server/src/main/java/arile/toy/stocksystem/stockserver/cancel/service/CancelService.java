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
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
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
    private final ReserveAmountCalculator reserveAmountCalculator;

    @Transactional
    public void registerCancel(CancelRequestEvent request) {

        UpdateOrderStatusResult result = orderService.updateOrderStatusByCancelEvent(request.orderId());

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

    private void cancelInternal(OrderEntity orderEntity) {

        boolean refunded;
        LeverageRatio leverageRatio = orderEntity.getLeverageRatio();

        if (orderEntity.getOrderType() == OrderType.BUY) {

            long orderAmount = (long) orderEntity.getOrderPrice() * orderEntity.getRemainingQuantity();

            // 원금(증거금)은 남은 수량 기준으로 그대로 재계산: 반올림 대상이 아니라 안전.
            // 수수료만 재계산(reserveAmountCalculator.calculateFee) 대신 OrderEntity에 정확히
            // 남아있는 remainingReservedFee를 그대로 환불: 반올림 오차 원천 차단
            long principal = leverageRatio.isSpot() ? orderAmount : leverageRatio.calculateMarginDeposit(orderAmount);
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

        cancelRepository.save(
                CancelEntity.of(orderEntity.getOrderId())
        );

        orderQueueRegistry.orderCancel(
                orderEntity.getOrderId(),
                orderEntity.getStockCode()
        );

        queuePositionBroadcastService.broadcast(orderEntity.getStockCode(), orderEntity.getOrderType());
        
        otocoOrderLifecycleListener.onOrderCanceled(orderEntity.getOrderId());
    }

    private void publishSuccess(OrderEntity orderEntity) {

        CancelResponseEvent event = CancelResponseEvent.of(orderEntity, true, null);

        stockServerOrderResponseRepository.delete(event.username(), event.orderId());

        cancelResponseEventPublisher.publish(event);
    }
}
