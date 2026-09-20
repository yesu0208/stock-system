package arile.toy.stocksystem.bffserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankUpdatedPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void push() {
        try {
            String json = objectMapper.writeValueAsString(RankUpdatedPushMessage.of());
            messagingTemplate.convertAndSend("/sub/market/rank", json);
        } catch (Exception e) {
            log.warn("RankUpdatedPushService push error", e);
        }
    }
}
