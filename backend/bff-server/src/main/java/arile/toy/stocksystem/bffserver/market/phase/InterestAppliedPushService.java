package arile.toy.stocksystem.bffserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestAppliedPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper; // [신규]

    public void push() {
        try {
            String json = objectMapper.writeValueAsString(InterestAppliedPushMessage.of());
            messagingTemplate.convertAndSend("/sub/market/interest", json);
        } catch (Exception e) {
            log.warn("InterestAppliedPushService push error", e);
        }
    }
}
