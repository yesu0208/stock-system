package arile.toy.stocksystem.bffserver.market.phase;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketCloseEventPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void push(String message) {
        messagingTemplate.convertAndSend(
                "/sub/market/close",
                message);
    }
}
