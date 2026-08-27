package arile.toy.stocksystem.stockserver.chart.event.publisher;

import arile.toy.stocksystem.stockserver.chart.event.DailyCandleUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDailyCandleEventPublisher {

    private final RedisTemplate<String, DailyCandleUpdateEvent> dailyCandleUpdateEventRedisTemplate;

    public void publish(DailyCandleUpdateEvent event) {
        try {
            dailyCandleUpdateEventRedisTemplate.convertAndSend(
                    channel(event.stockCode()), event);
        } catch (Exception e) {
            log.warn("dailyCandleUpdateEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String channel(String stockCode) {
        return "dailycandle." + stockCode + ":event";
    }
}
