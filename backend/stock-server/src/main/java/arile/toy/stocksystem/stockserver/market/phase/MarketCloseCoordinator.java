package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.sharding.StockGroupProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MarketCloseCoordinator {

    private static final String CLOSE_GROUP = "CLOSE";
    private static final Duration TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final StockGroupProperties stockGroupProperties;

    private String countKey() {
        return "market:close:done-count:" + LocalDate.now();
    }

    private long totalTradingGroups() {
        return stockGroupProperties.getGroups().keySet().stream()
                .filter(group -> !CLOSE_GROUP.equals(group))
                .count();
    }

    public boolean markDoneAndCheckLast() {
        Long count = redisTemplate.opsForValue().increment(countKey());
        redisTemplate.expire(countKey(), TTL);
        return count != null && count >= totalTradingGroups();
    }
}
