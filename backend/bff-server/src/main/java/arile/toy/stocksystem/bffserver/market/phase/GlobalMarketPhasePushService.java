package arile.toy.stocksystem.bffserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalMarketPhasePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void push(String phaseStr) {
        try {
            BffServerMarketPhase phase = BffServerMarketPhase.valueOf(phaseStr);
            String json = objectMapper.writeValueAsString(MarketPhasePushMessage.of(phase));
            messagingTemplate.convertAndSend("/sub/market/phase", json);
        } catch (Exception e) {
            log.warn("GlobalMarketPhasePushService push error. phaseStr={}", phaseStr, e);
        }
    }
}
