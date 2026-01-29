package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BffServerRedisBidAskPriceRepository implements  BffServerBidAskPriceRepository {

    private static final String KEY_PREFIX = "bidask:";

    private final RedisTemplate<String, BffServerBidAskPriceTickMessage> redisTemplate;

    public BffServerBidAskPriceTickMessage findByStockCode(String stockCode) {
        return redisTemplate.opsForValue().get(key(stockCode));
    }

    private String key(String stockCode) {
        return KEY_PREFIX + stockCode;
    }
}
