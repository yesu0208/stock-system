package arile.toy.stocksystem.stockserver.alert.repository;

import arile.toy.stocksystem.stockserver.alert.dto.StockServerAlertResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisAlertResponseRepository implements StockServerAlertResponseRepository {

    private static final String KEY_PREFIX = "user:alert:";

    private final RedisTemplate<String, StockServerAlertResponseMessage> redisTemplate;

    public void save(StockServerAlertResponseMessage stockServerAlertResponseMessage) {
        redisTemplate.opsForHash()
                .put(key(stockServerAlertResponseMessage.username()), stockServerAlertResponseMessage.alertId().toString(), stockServerAlertResponseMessage);
    }

    public void delete(String username, Long alertId) {
        redisTemplate.opsForHash().delete(key(username), alertId.toString());
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
