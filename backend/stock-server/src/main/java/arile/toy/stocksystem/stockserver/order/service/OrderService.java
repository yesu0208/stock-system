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

        if (!fromAutoOrder && request.orderType() == OrderType.BUY) {
            boolean reserved = accountApiClient
                    .reserveCash(request.username(), orderAmount);
            if (!reserved) {
                orderResponseEventPublisher.publishError(request, OrderErrorCode.INSUFFICIENT_BALANCE);
                return;
            }
        }  else if (!fromAutoOrder && request.orderType() == OrderType.SELL){
            boolean reserved = accountApiClient
                    .reserveStock(request.username(), request.stockCode(), request.orderQuantity());

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
                    accountApiClient.refundReservedCash(request.username(), orderAmount);
                } else {
                    accountApiClient.refundReservedStock(
                            request.username(), request.stockCode(), request.orderQuantity());
                }
            }
            orderResponseEventPublisher.publishError(request, OrderErrorCode.INTERNAL_ERROR);
            throw e;
        }

        var orderResponseMessage = new StockServerOrderResponseMessage(savedOrder.getOrderId(),
                savedOrder.getUsername(), savedOrder.getStockCode(),
                savedOrder.getOrderType(), savedOrder.getOrderPrice(),
                savedOrder.getOrderQuantity(), savedOrder.getRemainingQuantity(),
                savedOrder.getOrderTime());

        stockServerOrderResponseRepository.save(orderResponseMessage);
        orderResponseEventPublisher.publish(orderResponseMessage);
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
