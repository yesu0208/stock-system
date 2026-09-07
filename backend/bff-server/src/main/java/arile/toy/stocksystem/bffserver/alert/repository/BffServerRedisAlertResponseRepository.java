package arile.toy.stocksystem.bffserver.alert.repository;

import arile.toy.stocksystem.bffserver.alert.dto.AlertResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BffServerRedisAlertResponseRepository implements BffServerAlertResponseRepository {

    private static final String KEY_PREFIX = "user:alert:";

    private final RedisTemplate<String, AlertResponseMessage> redisTemplate;

    public List<AlertResponseMessage> findAll(String username) {
        return redisTemplate.opsForHash()
                .values(key(username))
                .stream()
                .map(o -> (AlertResponseMessage) o)
                .toList();
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
