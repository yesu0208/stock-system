package arile.toy.stocksystem.bffserver.order.repository;

import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BffServerRedisOrderResponseRepository implements BffServerOrderResponseRepository {

    private static final String KEY_PREFIX = "user:order:";

    private final RedisTemplate<String, OrderResponseMessage> redisTemplate;

    public List<OrderResponseMessage> findAll(String username) {
        return redisTemplate.opsForHash()
                .values(key(username))
                .stream()
                .map(o -> (OrderResponseMessage) o)
                .toList();
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
