package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisTradePriceRepository implements StockServerTradePriceRepository {

    private static final String KEY_PREFIX = "trade:";

    private final RedisTemplate<String, TradePriceTickMessage> redisTemplate;

    public void save(TradePriceTickMessage message) {
        redisTemplate.opsForValue().set(
                key(message.stockCode()),
                message
        );
    }

    private String key(String stockCode) {
        return KEY_PREFIX + stockCode;
    }
}
