package arile.toy.stocksystem.stockserver.market.phase;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MarketCloseLock {

    private final StringRedisTemplate redisTemplate;

    @Value("${server.group}")
    private String stockGroup;

    private static final String LOCK_KEY_PREFIX = "lock:market:close:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    public boolean acquire() {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue()
                        .setIfAbsent(lockKey(), "LOCKED", LOCK_TTL)
        );
    }

    public void release() {
        redisTemplate.delete(lockKey());
    }

    private String lockKey() {
        return LOCK_KEY_PREFIX + stockGroup;
    }
}
