package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.order.dto.OrderRequest;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponse;
import arile.toy.stocksystem.bffserver.order.event.OrderRequestEvent;
import arile.toy.stocksystem.bffserver.order.event.publisher.OrderRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderIngressService {

    private final OrderRequestEventPublisher orderRequestEventPublisher;

    public OrderResponse receive(String username, OrderRequest request) {

        OrderRequestEvent event = new OrderRequestEvent(
                username,
                request.stockCode(),
                request.orderType(),
                request.orderPrice(),
                request.orderQuantity()
        );

        orderRequestEventPublisher.publishOrder(event);

        return new OrderResponse(username, request.stockCode(),  request.orderType(), request.orderPrice(), request.orderQuantity());
    }
}
