package arile.toy.stocksystem.stockserver.otococancel.event.publisher;

import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOtocoCancelResponseEventPublisher implements OtocoCancelResponseEventPublisher {

    private final RedisTemplate<String, OtocoCancelResponseEvent> redisOtocoCancelResponseEventRedisTemplate;

    public void publish(OtocoCancelResponseEvent event) {
        try {
            String channel = resolveChannel(event.username());
            redisOtocoCancelResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisOtocoCancelResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:otoco:cancel." + username + ":event";
    }
}
