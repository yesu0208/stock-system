package arile.toy.stocksystem.stockserver.market.phase;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MarketCloseLock {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "lock:market:close";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    public boolean acquire() {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue()
                        .setIfAbsent(LOCK_KEY, "LOCKED", LOCK_TTL)
        );
    }

    public void release() {
        redisTemplate.delete(LOCK_KEY);
    }
}
