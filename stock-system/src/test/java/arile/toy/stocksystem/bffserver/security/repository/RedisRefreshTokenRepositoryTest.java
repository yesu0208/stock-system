package arile.toy.stocksystem.bffserver.security.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRefreshTokenRepository repository;

    private static final String PREFIX = "refresh:";

    @Test
    @DisplayName("JTI와 사용자명으로 저장 시 Redis set 호출 및 TTL 적용")
    void givenJtiAndUsername_whenSave_thenCallsRedisSetWithTtl() {
        // given
        String jti = "token123";
        String username = "testUser";
        long ttlMillis = 1000L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        repository.save(jti, username, ttlMillis);

        // then
        verify(valueOperations).set(PREFIX + jti, username, Duration.ofMillis(ttlMillis));
    }

    @Test
    @DisplayName("존재하는 JTI 조회 시 true 반환")
    void givenExistingJti_whenExists_thenReturnsTrue() {
        // given
        String jti = "token123";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(true);

        // when
        boolean result = repository.exists(jti);

        // then
        assertTrue(result);
        verify(redisTemplate).hasKey(PREFIX + jti);
    }

    @Test
    @DisplayName("존재하지 않는 JTI 조회 시 false 반환")
    void givenNonExistingJti_whenExists_thenReturnsFalse() {
        // given
        String jti = "token456";
        when(redisTemplate.hasKey(PREFIX + jti)).thenReturn(false);

        // when
        boolean result = repository.exists(jti);

        // then
        assertFalse(result);
        verify(redisTemplate).hasKey(PREFIX + jti);
    }

    @Test
    @DisplayName("JTI 삭제 시 Redis delete 호출")
    void givenJti_whenDelete_thenCallsRedisDelete() {
        // given
        String jti = "token123";

        // when
        repository.delete(jti);

        // then
        verify(redisTemplate).delete(PREFIX + jti);
    }
}
