package arile.toy.stocksystem.accountserver.stockprice.repository;

import arile.toy.stocksystem.accountserver.stockprice.dto.StockSummaryTickMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockSummaryRedisRepository {

    private static final String KEY = "stock:summary";

    private final RedisTemplate<String, StockSummaryTickMessage> stockSummaryTickMessageRedisTemplate;

    public StockSummaryTickMessage findByStockCode(String stockCode) {
        return (StockSummaryTickMessage) stockSummaryTickMessageRedisTemplate
                .opsForHash().get(KEY, stockCode);
    }
}
