package arile.toy.stocksystem.stockserver.otoco.repository;

import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisOtocoResponseRepository implements StockServerOtocoResponseRepository {

    private static final String KEY_PREFIX = "user:otoco:";

    private final RedisTemplate<String, StockServerOtocoResponseMessage> redisTemplate;

    public void save(StockServerOtocoResponseMessage message) {
        redisTemplate.opsForHash().put(key(message.username()), message.otocoId().toString(), message);
    }

    public void update(String username, Long otocoId, StockServerOtocoResponseMessage newValue) {
        redisTemplate.opsForHash().put(key(username), otocoId.toString(), newValue);
    }

    public void delete(String username, Long otocoId) {
        redisTemplate.opsForHash().delete(key(username), otocoId.toString());
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
