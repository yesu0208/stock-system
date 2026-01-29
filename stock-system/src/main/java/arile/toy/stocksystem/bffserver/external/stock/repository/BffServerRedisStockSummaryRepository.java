package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BffServerRedisStockSummaryRepository implements BffServerStockSummaryRepository {

    private static final String KEY = "stock:summary";

    private final RedisTemplate<String, BffServerStockSummaryTickMessage> redisTemplate;

    public BffServerStockSummaryTickMessage findByStockCode(String stockCode) {
        return (BffServerStockSummaryTickMessage) redisTemplate.opsForHash()
                .get(KEY, stockCode);
    }

    public List<BffServerStockSummaryTickMessage> findAll() {
        return redisTemplate.opsForHash()
                .values(KEY)
                .stream()
                .map(v -> (BffServerStockSummaryTickMessage) v)
                .toList();
    }
}
