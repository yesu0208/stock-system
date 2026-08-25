package arile.toy.stocksystem.bffserver.security.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements  RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "refresh:";

    @Override
    public void save(String jti, String username, long ttlMillis) {
        redisTemplate.opsForValue().set(
                PREFIX + jti,
                username,
                Duration.ofMillis(ttlMillis)
        );
    }

    @Override
    public boolean exists(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + jti));
    }

    @Override
    public void delete(String jti) {
        redisTemplate.delete(PREFIX + jti);
    }
}
