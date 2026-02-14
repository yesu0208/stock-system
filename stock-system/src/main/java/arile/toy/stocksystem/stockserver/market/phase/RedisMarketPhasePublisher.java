package arile.toy.stocksystem.stockserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMarketPhasePublisher implements MarketPhasePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL = "market-phase";

    public void publish(String stockCode, StockServerMarketPhase phase) {

        try {
            var marketPhaseEvent = MarketPhaseEvent.of(stockCode, phase);

            String message = objectMapper.writeValueAsString(marketPhaseEvent);

            redisTemplate.convertAndSend(CHANNEL, message);

        } catch (Exception e) {
            throw new RuntimeException("MarketPhase publish failed", e);
        }
    }
}
