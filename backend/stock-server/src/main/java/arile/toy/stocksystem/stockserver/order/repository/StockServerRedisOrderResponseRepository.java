package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServerRedisOrderResponseRepository implements StockServerOrderResponseRepository {

    private static final String KEY_PREFIX = "user:order:";

    private final RedisTemplate<String, StockServerOrderResponseMessage> redisTemplate;

    public void save(StockServerOrderResponseMessage stockServerOrderResponseMessage) {
        redisTemplate.opsForHash()
                .put(key(stockServerOrderResponseMessage.username()), stockServerOrderResponseMessage.orderId().toString(), stockServerOrderResponseMessage);
    }

    public void delete(String username, Long orderId) {
        redisTemplate.opsForHash().delete(key(username), orderId.toString());
    }


    public void update(String username, Long orderId, StockServerOrderResponseMessage newValue) {
        redisTemplate.opsForHash()
                .put(key(username), orderId.toString(), newValue);
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
