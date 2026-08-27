package arile.toy.stocksystem.stockserver.chart.event.publisher;

import arile.toy.stocksystem.stockserver.chart.event.MinuteCandleUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMinuteCandleEventPublisher {

    private final RedisTemplate<String, MinuteCandleUpdateEvent> minuteCandleUpdateEventRedisTemplate;

    public void publish(MinuteCandleUpdateEvent event) {
        try {
            minuteCandleUpdateEventRedisTemplate.convertAndSend(
                    channel(event.stockCode()), event);
        } catch (Exception e) {
            log.warn("minuteCandleUpdateEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String channel(String stockCode) {
        return "minutecandle." + stockCode + ":event";
    }
}
