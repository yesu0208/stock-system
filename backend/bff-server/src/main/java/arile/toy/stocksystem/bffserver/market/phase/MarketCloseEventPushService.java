package arile.toy.stocksystem.bffserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCloseEventPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void push(String session) {
        try {
            String json = objectMapper.writeValueAsString(MarketClosePushMessage.of(session));
            messagingTemplate.convertAndSend("/sub/market/close", json);
        } catch (Exception e) {
            log.warn("MarketCloseEventPushService push error. session={}", session, e);
        }
    }
}
