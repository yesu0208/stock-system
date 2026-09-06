package arile.toy.stocksystem.bffserver.otoco.repository;

import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BffServerRedisOtocoResponseRepository implements BffServerOtocoResponseRepository {

    private static final String KEY_PREFIX = "user:otoco:";

    private final RedisTemplate<String, OtocoResponseMessage> redisTemplate;

    public List<OtocoResponseMessage> findAll(String username) {
        return redisTemplate.opsForHash()
                .values(key(username))
                .stream()
                .map(o -> (OtocoResponseMessage) o)
                .toList();
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
