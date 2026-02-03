package arile.toy.stocksystem.bffserver.trade.service;

import arile.toy.stocksystem.bffserver.trade.dto.TradeResponse;
import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeResponsePushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void push(TradeResponseEvent tradeResponseEvent) {

        messagingTemplate.convertAndSendToUser(
                tradeResponseEvent.username(),
                "/sub/trade",
                TradeResponse.fromEvent(tradeResponseEvent)
        );
    }
}
