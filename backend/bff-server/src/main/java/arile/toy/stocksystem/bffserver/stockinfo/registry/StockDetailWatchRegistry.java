package arile.toy.stocksystem.bffserver.stockinfo.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class StockDetailWatchRegistry {

    private static final String KEY = "stock:detail:watch";
    private static final long STALE_THRESHOLD_MS = 15_000; // heartbeat 주기(10s)보다 여유있게

    private final StringRedisTemplate redisTemplate;

    public void heartbeat(String stockCode) {
        redisTemplate.opsForZSet().add(KEY, stockCode, System.currentTimeMillis());
    }

    public Set<String> getActiveCodes() {
        double min = System.currentTimeMillis() - STALE_THRESHOLD_MS;
        return redisTemplate.opsForZSet().rangeByScore(KEY, min, Double.MAX_VALUE);
    }
}
