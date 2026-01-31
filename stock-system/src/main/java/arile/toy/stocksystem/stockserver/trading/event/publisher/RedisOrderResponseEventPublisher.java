package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.event.OrderResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOrderResponseEventPublisher implements OrderResponseEventPublisher {

    private final RedisTemplate<String, OrderResponseEvent> redisOrderResponseEventRedisTemplate;

    public void publish(String username) {
        try {
            OrderResponseEvent event = new OrderResponseEvent(username, true, null);
            String channel = resolveChannel(username);

            redisOrderResponseEventRedisTemplate.convertAndSend(
                    channel,
                    event
            );
        } catch (Exception e) {
            log.warn("redisOrderResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:order." + username + ":event";
    }
}
