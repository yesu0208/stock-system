package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisStockSummaryRepository implements StockServerStockSummaryRepository {

    private static final String KEY = "stock:summary";

    private final RedisTemplate<String, StockSummaryTickMessage> redisTemplate;

    public void save(StockSummaryTickMessage message) {
        redisTemplate.opsForHash()
                .put(KEY, message.stockCode(), message);
    }

    public StockSummaryTickMessage findByStockCode(String stockCode) {
        return (StockSummaryTickMessage) redisTemplate.opsForHash()
                .get(KEY, stockCode);
    }
}
