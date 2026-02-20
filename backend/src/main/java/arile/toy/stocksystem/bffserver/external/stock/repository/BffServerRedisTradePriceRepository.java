package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BffServerRedisTradePriceRepository implements  BffServerTradePriceRepository {

    private static final String KEY_PREFIX = "trade:";

    private final RedisTemplate<String, BffServerTradePriceTickMessage> redisTemplate;

    public BffServerTradePriceTickMessage findByStockCode(String stockCode) {
        return redisTemplate.opsForValue().get(key(stockCode));
    }

    private String key(String stockCode) {
        return KEY_PREFIX + stockCode;
    }
}
