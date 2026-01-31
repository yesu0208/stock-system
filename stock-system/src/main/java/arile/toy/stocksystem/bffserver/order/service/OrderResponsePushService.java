package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.trading.event.OrderResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderResponsePushService {

    private final BffServerOrderResponseRepository bffServerOrderResponseRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void push(OrderResponseEvent orderResponseEvent) {

        List<OrderResponseMessage> responses
                = bffServerOrderResponseRepository.findAll(orderResponseEvent.username());
        messagingTemplate.convertAndSendToUser(
                orderResponseEvent.username(),
                "/sub/order",
                responses
        );
    }
}
