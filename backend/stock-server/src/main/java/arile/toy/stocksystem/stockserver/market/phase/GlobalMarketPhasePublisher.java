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
    private static final String SNAPSHOT_KEY = "market:global-phase:snapshot";

    public void publish(StockServerMarketPhase phase) {
        try {
            redisTemplate.opsForValue().set(SNAPSHOT_KEY, phase.name());
        } catch (Exception e) {
            log.warn("global market phase snapshot 저장 실패", e);
        }

        try {
            redisTemplate.convertAndSend(CHANNEL, phase.name());
            log.info("[GlobalMarketPhasePublisher] Published global phase: {}", phase);
        } catch (Exception e) {
            log.warn("publishGlobalPhase error", e);
        }
    }
}
