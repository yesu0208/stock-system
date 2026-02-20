package arile.toy.stocksystem.stockserver.trade.event.publisher;

import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTradeResponseEventPublisher implements TradeResponseEventPublisher {

    private final RedisTemplate<String, TradeResponseEvent> redisTradeResponseEventRedisTemplate;

    public void publish(TradeResponseEvent tradeResponseEvent) {
        try {
            String channel = resolveChannel(tradeResponseEvent.username());

            redisTradeResponseEventRedisTemplate.convertAndSend(
                    channel,
                    tradeResponseEvent
            );
        } catch (Exception e) {
            log.warn("redisTradeResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:trade." + username + ":event";
    }
}
