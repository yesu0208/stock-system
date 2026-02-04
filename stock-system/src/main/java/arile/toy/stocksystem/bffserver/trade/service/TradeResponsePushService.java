package arile.toy.stocksystem.bffserver.trade.service;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import arile.toy.stocksystem.bffserver.trade.dto.TradeResponse;
import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BffServerOrderResponseRepository bffServerOrderResponseRepository;

    public void push(TradeResponseEvent tradeResponseEvent) {

        messagingTemplate.convertAndSendToUser(
                tradeResponseEvent.username(),
                "/sub/trade",
                TradeResponse.fromEvent(tradeResponseEvent)
        );

        List<OrderResponseMessage> responses
                = bffServerOrderResponseRepository.findAll(tradeResponseEvent.username());

        messagingTemplate.convertAndSendToUser(
                tradeResponseEvent.username(),
                "/sub/order",
                responses
        );
    }
}
