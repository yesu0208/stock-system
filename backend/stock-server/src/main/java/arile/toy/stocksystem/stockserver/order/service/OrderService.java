package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.event.publisher.OrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderQueueRegistry orderQueueRegistry;
    private final OrderResponseEventPublisher orderResponseEventPublisher;
    private final StockServerOrderResponseRepository stockServerOrderResponseRepository;
    private final AccountApiClient accountApiClient;

    public void registerOrder(StockServerOrderRequestEvent request, boolean fromAutoOrder) {

        long orderAmount = (long) request.orderPrice() * request.orderQuantity();
        LeverageRatio leverageRatio = request.leverageRatio() == null ? LeverageRatio.SPOT : request.leverageRatio();

        // 레버리지 매수 시 실제 예약해야 할 현금은 "매수금액 전체"가 아니라 "개시증거금"만큼
        long reserveAmount = resolveReserveAmount(request.orderType(), leverageRatio, orderAmount);

        if (!fromAutoOrder && request.orderType() == OrderType.BUY) {
            boolean reserved = accountApiClient
                    .reserveCash(request.username(), reserveAmount);
            if (!reserved) {
                orderResponseEventPublisher.publishError(request, OrderErrorCode.INSUFFICIENT_BALANCE);
                return;
            }
        } else if (!fromAutoOrder && request.orderType() == OrderType.SELL) {
            // 레버리지 매도는 현물 재고가 아니라 레버리지 포지션 수량을 검증해야 하므로 별도 분기
            boolean reserved = leverageRatio.isSpot()
                    ? accountApiClient.reserveStock(request.username(), request.stockCode(), request.orderQuantity())
                    : accountApiClient.reserveLeverageStock(request.username(), request.stockCode(), leverageRatio.name(), request.orderQuantity());

            if (!reserved) {
                orderResponseEventPublisher.publishError(request, OrderErrorCode.INSUFFICIENT_STOCK);
                return;
            }
        }

        OrderEntity savedOrder;

        try {
            OrderEntity orderEntity = OrderEntity.of(
                    request.username(),
                    request.stockCode(),
                    request.orderType(),
                    leverageRatio,
                    request.orderPrice(),
                    request.orderQuantity(),
                    OrderStatus.OPEN,
                    request.orderQuantity()
            );
            savedOrder = orderRepository.save(orderEntity);

            var orderDto = OrderDto.fromEntity(savedOrder);
            orderQueueRegistry.orderEnqueue(orderDto);

        } catch (Exception e) {
            if (!fromAutoOrder) {
                if (request.orderType() == OrderType.BUY) {
                    accountApiClient.refundReservedCash(request.username(), reserveAmount);
                } else {
                    if (leverageRatio.isSpot()) {
                        accountApiClient.refundReservedStock(
                                request.username(), request.stockCode(), request.orderQuantity());
                    } else {
                        accountApiClient.refundReservedLeverageStock(
                                request.username(), request.stockCode(), leverageRatio.name(), request.orderQuantity());
                    }
                }
            }
            orderResponseEventPublisher.publishError(request, OrderErrorCode.INTERNAL_ERROR);
            throw e;
        }

        var orderResponseMessage = new StockServerOrderResponseMessage(savedOrder.getOrderId(),
                savedOrder.getUsername(), savedOrder.getStockCode(),
                savedOrder.getOrderType(), savedOrder.getLeverageRatio(), savedOrder.getOrderPrice(),
                savedOrder.getOrderQuantity(), savedOrder.getRemainingQuantity(),
                savedOrder.getOrderTime());

        stockServerOrderResponseRepository.save(orderResponseMessage);
        orderResponseEventPublisher.publish(orderResponseMessage);
    }

    /** 레버리지 매수 시 예약해야 할 실제 현금(개시증거금)을 계산 */
    private long resolveReserveAmount(OrderType orderType, LeverageRatio leverageRatio, long orderAmount) {
        if (orderType != OrderType.BUY || leverageRatio.isSpot()) {
            return orderAmount;
        }
        return leverageRatio.calculateMarginDeposit(orderAmount);
    }

    @Transactional
    public UpdateOrderStatusResult updateOrderStatusByCancelEvent(Long orderId) {

        OrderEntity orderEntity = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found"));

        OrderStatus prevStatus = orderEntity.getOrderStatus();

        if (prevStatus == OrderStatus.CANCELED ||
                prevStatus == OrderStatus.FILLED) {
            return UpdateOrderStatusResult.of(orderEntity, prevStatus);
        }

        orderEntity.changeOrderStatus(OrderStatus.CANCELED);
        return UpdateOrderStatusResult.of(orderEntity, prevStatus);
    }

    @Transactional
    public List<OrderEntity> findAllUnfilledOrders(List<String> stockCodes) {
        return orderRepository.findAllUnfilled(stockCodes);
    }
}
