package arile.toy.stocksystem.stockserver.market.phase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketClosePublisher {

    private final StringRedisTemplate redisTemplate;
    private static final String MARKET_CLOSE_CHANNEL = "market:close";

    public void publishMarketClose() {

        try {

            String message = "MARKET_CLOSED";

            redisTemplate.convertAndSend(MARKET_CLOSE_CHANNEL, message);
            log.info("[RedisPublisher] Published market close event to channel {}", MARKET_CLOSE_CHANNEL);
        } catch (Exception e) {
            log.warn("publishMarketClose.convertAndSend error", e);
        }
    }
}
