package arile.toy.stocksystem.stockserver.external.stock.event.publisher;

import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTradePriceEventPublisher {

    private final RedisTemplate<String, TradePriceTickEvent>  tradePriceTickEventRedisTemplate;

    public void publish(TradePriceTickEvent tradePriceTickEvent) {
        try {
            String channel = resolveChannel(tradePriceTickEvent.stockCode());

            tradePriceTickEventRedisTemplate.convertAndSend(
                    channel,
                    tradePriceTickEvent
            );
        } catch (Exception e) {
            log.warn("tradePriceTickEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String stockCode) {
        return "trade." + stockCode + ":event";
    }
}
