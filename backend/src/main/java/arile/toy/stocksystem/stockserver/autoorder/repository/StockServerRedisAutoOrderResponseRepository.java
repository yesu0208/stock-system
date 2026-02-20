package arile.toy.stocksystem.stockserver.autoorder.repository;

import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisAutoOrderResponseRepository implements StockServerAutoOrderResponseRepository {

    private static final String KEY_PREFIX = "user:auto:order:";

    private final RedisTemplate<String, StockServerAutoOrderResponseMessage> redisTemplate;

    public void save(StockServerAutoOrderResponseMessage stockServerAutoOrderResponseMessage) {
        redisTemplate.opsForHash()
                .put(key(stockServerAutoOrderResponseMessage.username()), stockServerAutoOrderResponseMessage.autoOrderId().toString(), stockServerAutoOrderResponseMessage);
    }

    public void delete(String username, Long autoOrderId) {
        redisTemplate.opsForHash().delete(key(username), autoOrderId.toString());
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
