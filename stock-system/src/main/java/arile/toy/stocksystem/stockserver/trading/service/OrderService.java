package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.trading.dto.order.*;
import arile.toy.stocksystem.stockserver.trading.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.trading.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.trading.event.publisher.OrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.trading.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderQueueRegistry orderQueueRegistry;
    private final OrderResponseEventPublisher orderResponseEventPublisher;
    private final StockServerOrderResponseRepository stockServerOrderResponseRepository;
    private final AccountBalanceCommand accountBalanceCommand;

    public void registerOrder(StockServerOrderRequestEvent request) {

        long orderAmount = (long) request.orderPrice() * request.orderQuantity();

        boolean reserved = accountBalanceCommand
                .reserveCash(request.username(), orderAmount);

        if (!reserved) {
            // Todo: client에게 잔고 부족 알리기
            return;
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
            accountBalanceCommand.refundReservedCash(request.username(), orderAmount);
            throw e;
        }

        var orderResponseMessage = new StockServerOrderResponseMessage(savedOrder.getOrderId(),
                savedOrder.getUsername(), savedOrder.getStockCode(),
                savedOrder.getOrderType(), savedOrder.getOrderPrice(),
                savedOrder.getOrderQuantity(), savedOrder.getRemainingQuantity(),
                savedOrder.getOrderTime());

        stockServerOrderResponseRepository.save(orderResponseMessage);
        orderResponseEventPublisher.publish(request.username());
    }
}
