package arile.toy.stocksystem.stockserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMarketPhasePublisher implements MarketPhasePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL = "market-phase";
    private static final String SNAPSHOT_KEY = "market:phase:snapshot";

    public void publish(String stockCode, StockServerMarketPhase phase) {

        try {
            redisTemplate.opsForHash().put(SNAPSHOT_KEY, stockCode, phase.name());
        } catch (Exception e) {
            log.warn("market phase snapshot 저장 실패. stockCode={}, phase={}", stockCode, phase, e);
        }

        try {
            var marketPhaseEvent = MarketPhaseEvent.of(stockCode, phase);
            String message = objectMapper.writeValueAsString(marketPhaseEvent);
            redisTemplate.convertAndSend(CHANNEL, message);
        } catch (Exception e) {
            log.warn("marketPhaseEvent.convertAndSend error", e);
        }
    }
}
