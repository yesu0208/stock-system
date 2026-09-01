package arile.toy.stocksystem.accountserver.leverage.event.publisher;

import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMarginCallEventPublisher implements MarginCallEventPublisher {

    private final RedisTemplate<String, MarginCallEvent> marginCallEventRedisTemplate;

    @Override
    public void publish(MarginCallEvent event) {
        try {
            String channel = resolveChannel(event.username());
            marginCallEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("marginCallEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:margincall." + username + ":event";
    }
}
