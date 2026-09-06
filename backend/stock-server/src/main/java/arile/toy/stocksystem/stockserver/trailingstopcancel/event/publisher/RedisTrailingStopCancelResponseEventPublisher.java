package arile.toy.stocksystem.stockserver.trailingstopcancel.event.publisher;

import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTrailingStopCancelResponseEventPublisher implements TrailingStopCancelResponseEventPublisher {

    private final RedisTemplate<String, TrailingStopCancelResponseEvent> redisTrailingStopCancelResponseEventRedisTemplate;

    public void publish(TrailingStopCancelResponseEvent event) {
        try {
            String channel = resolveChannel(event.username());

            redisTrailingStopCancelResponseEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("redisTrailingStopCancelResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:trailing:stop:cancel." + username + ":event";
    }
}
