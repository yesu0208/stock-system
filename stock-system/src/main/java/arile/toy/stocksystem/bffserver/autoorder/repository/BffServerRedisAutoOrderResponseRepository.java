package arile.toy.stocksystem.bffserver.autoorder.repository;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.trading.event.publisher.AutoOrderResponseEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BffServerRedisAutoOrderResponseRepository implements BffServerAutoOrderResponseRepository {

    private static final String KEY_PREFIX = "user:auto:order:";

    private final RedisTemplate<String, AutoOrderResponseMessage> redisTemplate;

    public List<AutoOrderResponseMessage> findAll(String username) {
        return redisTemplate.opsForHash()
                .values(key(username))
                .stream()
                .map(o -> (AutoOrderResponseMessage) o)
                .toList();
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
