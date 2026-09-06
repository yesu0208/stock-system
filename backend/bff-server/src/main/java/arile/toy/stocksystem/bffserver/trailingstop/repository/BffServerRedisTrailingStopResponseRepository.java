package arile.toy.stocksystem.bffserver.trailingstop.repository;

import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BffServerRedisTrailingStopResponseRepository implements BffServerTrailingStopResponseRepository {

    private static final String KEY_PREFIX = "user:trailing:stop:";

    private final RedisTemplate<String, TrailingStopResponseMessage> redisTemplate;

    public List<TrailingStopResponseMessage> findAll(String username) {
        return redisTemplate.opsForHash()
                .values(key(username))
                .stream()
                .map(o -> (TrailingStopResponseMessage) o)
                .toList();
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
