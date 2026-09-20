package arile.toy.stocksystem.stockserver.order.event.publisher;

import arile.toy.stocksystem.stockserver.order.event.QueuePositionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisQueuePositionEventPublisher implements QueuePositionEventPublisher {

    private final RedisTemplate<String, QueuePositionEvent> queuePositionEventRedisTemplate;

    @Override
    public void publish(QueuePositionEvent event) {
        try {
            String channel = resolveChannel(event.username());
            queuePositionEventRedisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("queuePositionEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:order:queue-position." + username + ":event";
    }
}
