package arile.toy.stocksystem.stockserver.alertcancel.event.publisher;

import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAlertCancelResponseEventPublisher implements AlertCancelResponseEventPublisher {

    private final RedisTemplate<String, AlertCancelResponseEvent> alertCancelResponseEventRedisTemplate;

    public void publish(AlertCancelResponseEvent alertCancelResponseEvent) {
        try {
            String channel = resolveChannel(alertCancelResponseEvent.username());

            alertCancelResponseEventRedisTemplate.convertAndSend(
                    channel,
                    alertCancelResponseEvent
            );
        } catch (Exception e) {
            log.warn("alertCancelResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:alert:cancel." + username + ":event";
    }
}
