package arile.toy.stocksystem.bffserver.autoorder.service;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderRequest;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponse;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderRequestEvent;
import arile.toy.stocksystem.bffserver.autoorder.event.publisher.AutoOrderRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoOrderIngressService {

    private final AutoOrderRequestEventPublisher publisher;

    public AutoOrderResponse receive(String username, AutoOrderRequest request) {

        AutoOrderRequestEvent event = new AutoOrderRequestEvent(
                username,
                request.stockCode(),
                request.autoOrderType(),
                request.triggerPrice(),
                request.orderPrice(),
                request.orderQuantity()
        );

        publisher.publishAutoOrder(event);

        return new AutoOrderResponse(username, request.stockCode(),  request.autoOrderType(),
                request.triggerPrice(), request.orderPrice(), request.orderQuantity());
    }
}
