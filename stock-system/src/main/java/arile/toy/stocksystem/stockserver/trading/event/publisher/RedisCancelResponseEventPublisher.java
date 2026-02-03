package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.event.CancelResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCancelResponseEventPublisher implements CancelResponseEventPublisher {

    private final RedisTemplate<String, CancelResponseEvent> redisCancelResponseEventRedisTemplate;

    public void publish(CancelResponseEvent cancelResponseEvent) {
        try {
            String channel = resolveChannel(cancelResponseEvent.username());

            redisCancelResponseEventRedisTemplate.convertAndSend(
                    channel,
                    cancelResponseEvent
            );
        } catch (Exception e) {
            log.warn("redisCancelResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:cancel." + username + ":event";
    }
}
