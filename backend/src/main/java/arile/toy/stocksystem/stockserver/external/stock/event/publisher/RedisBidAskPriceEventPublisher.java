package arile.toy.stocksystem.stockserver.external.stock.event.publisher;

import arile.toy.stocksystem.stockserver.external.stock.event.BidAskPriceTickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisBidAskPriceEventPublisher {

    private final RedisTemplate<String, BidAskPriceTickEvent> bidAskPriceTickEventRedisTemplate;

    public void publish(BidAskPriceTickEvent bidAskPriceTickEvent) {
        try {
            String channel = resolveChannel(bidAskPriceTickEvent.stockCode());

            bidAskPriceTickEventRedisTemplate.convertAndSend(
                    channel,
                    bidAskPriceTickEvent
            );
        } catch (Exception e) {
            log.warn("bidAskPriceTickEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String stockCode) {
        return "bidask." + stockCode + ":event";
    }
}
