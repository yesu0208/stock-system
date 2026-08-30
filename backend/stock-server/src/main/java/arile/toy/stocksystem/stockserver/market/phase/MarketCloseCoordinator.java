package arile.toy.stocksystem.stockserver.market.phase;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MarketCloseCoordinator {

    private final StringRedisTemplate redisTemplate;

    @Value("${market-close.total-groups}")
    private int totalGroups;

    private static final Duration TTL = Duration.ofHours(2);

    private String countKey() {
        return "market:close:done-count:" + LocalDate.now();
    }

    public boolean markDoneAndCheckLast() {
        Long count = redisTemplate.opsForValue().increment(countKey());
        redisTemplate.expire(countKey(), TTL);
        return count != null && count >= totalGroups;
    }
}
