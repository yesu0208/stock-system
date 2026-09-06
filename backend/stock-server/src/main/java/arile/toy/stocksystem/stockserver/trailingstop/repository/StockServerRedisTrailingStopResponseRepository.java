package arile.toy.stocksystem.stockserver.trailingstop.repository;

import arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisTrailingStopResponseRepository implements StockServerTrailingStopResponseRepository {

    private static final String KEY_PREFIX = "user:trailing:stop:";

    private final RedisTemplate<String, StockServerTrailingStopResponseMessage> redisTemplate;

    public void save(StockServerTrailingStopResponseMessage message) {
        redisTemplate.opsForHash().put(key(message.username()), message.trailingStopId().toString(), message);
    }

    public void update(String username, Long trailingStopId, StockServerTrailingStopResponseMessage newValue) {
        redisTemplate.opsForHash().put(key(username), trailingStopId.toString(), newValue);
    }

    public void delete(String username, Long trailingStopId) {
        redisTemplate.opsForHash().delete(key(username), trailingStopId.toString());
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
