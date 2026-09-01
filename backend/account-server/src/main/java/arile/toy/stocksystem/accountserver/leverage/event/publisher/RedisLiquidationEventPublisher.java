package arile.toy.stocksystem.accountserver.leverage.event.publisher;

import arile.toy.stocksystem.accountserver.leverage.event.LiquidationExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLiquidationEventPublisher implements LiquidationEventPublisher {

    private final RedisTemplate<String, LiquidationExecutedEvent> liquidationExecutedEventRedisTemplate;

    @Override
    public void publish(LiquidationExecutedEvent event) {
        try {
            String channel = "user:liquidation." + event.username() + ":event";
            liquidationExecutedEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("liquidationExecutedEventRedisTemplate.convertAndSend error", e);
        }
    }
}
