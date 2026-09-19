package arile.toy.stocksystem.stockserver.market.phase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalMarketPhasePublisher {

    private final StringRedisTemplate redisTemplate;
    private static final String CHANNEL = "market:global-phase";

    public void publish(StockServerMarketPhase phase) {
        try {
            redisTemplate.convertAndSend(CHANNEL, phase.name());
            log.info("[GlobalMarketPhasePublisher] Published global phase: {}", phase);
        } catch (Exception e) {
            log.warn("publishGlobalPhase error", e);
        }
    }
}
