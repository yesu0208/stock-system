package arile.toy.stocksystem.stockserver.autocancel.event.publisher;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAutoCancelResponseEventPublisher implements  AutoCancelResponseEventPublisher {

    private final RedisTemplate<String, AutoCancelResponseEvent> redisAutoCancelResponseEventRedisTemplate;

    public void publish(AutoCancelResponseEvent autoCancelResponseEvent) {
        try {
            String channel = resolveChannel(autoCancelResponseEvent.username());

            redisAutoCancelResponseEventRedisTemplate.convertAndSend(
                    channel,
                    autoCancelResponseEvent
            );
        } catch (Exception e) {
            log.warn("redisAutoCancelResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:auto:cancel." + username + ":event";
    }
}
