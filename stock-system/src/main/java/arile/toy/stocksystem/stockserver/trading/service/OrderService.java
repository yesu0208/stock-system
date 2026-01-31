package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.trading.dto.order.*;
import arile.toy.stocksystem.stockserver.trading.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.trading.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.trading.event.publisher.OrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.trading.repository.StockServerOrderResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderResponseEventPublisher orderResponseEventPublisher;
    private final StockServerOrderResponseRepository stockServerOrderResponseRepository;

    public void registerOrder(StockServerOrderRequestEvent request) {

        OrderEntity orderEntity = OrderEntity.of(
                request.username(),
                request.stockCode(),
                request.orderType(),
                request.orderPrice(),
                request.orderQuantity(),
                OrderStatus.OPEN,
                request.orderQuantity()
        );
        OrderEntity savedOrder = orderRepository.save(orderEntity);

        var orderResponseMessage = new StockServerOrderResponseMessage(savedOrder.getOrderId(),
                savedOrder.getUsername(), savedOrder.getStockCode(),
                savedOrder.getOrderType(), savedOrder.getOrderPrice(),
                savedOrder.getOrderQuantity(), savedOrder.getRemainingQuantity(),
                savedOrder.getOrderTime());

        stockServerOrderResponseRepository.save(orderResponseMessage);
        orderResponseEventPublisher.publish(request.username());
    }
}
