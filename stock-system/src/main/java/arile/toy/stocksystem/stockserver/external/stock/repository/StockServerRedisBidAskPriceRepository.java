package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisBidAskPriceRepository implements StockServerBidAskPriceRepository{

    private static final String KEY_PREFIX = "bidask:";

    private final RedisTemplate<String, BidAskPriceTickMessage> redisTemplate;

    public void save(BidAskPriceTickMessage message) {
        redisTemplate.opsForValue().set(
                key(message.stockCode()),
                message
        );
    }

    private String key(String stockCode) {
        return KEY_PREFIX + stockCode;
    }
}
