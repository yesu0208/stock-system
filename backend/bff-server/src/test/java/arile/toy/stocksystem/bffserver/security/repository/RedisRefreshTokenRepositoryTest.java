package arile.toy.stocksystem.bffserver.security.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisRefreshTokenRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RedisRefreshTokenRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        repository = new RedisRefreshTokenRepository(redisTemplate);
    }

    @Test
    @DisplayName("save: refresh:{jti} 키에 사용자명을 TTL과 함께 저장한다")
    void save() {
        repository.save("jti-1", "user1", 604_800_000L);

        verify(valueOps).set("refresh:jti-1", "user1", Duration.ofMillis(604_800_000L));
    }

    @Test
    @DisplayName("exists: 키가 있으면 true, 없거나 결과가 null이면 false")
    void exists() {
        given(redisTemplate.hasKey("refresh:jti-1")).willReturn(true);
        given(redisTemplate.hasKey("refresh:jti-2")).willReturn(false);
        given(redisTemplate.hasKey("refresh:jti-3")).willReturn(null);

        assertThat(repository.exists("jti-1")).isTrue();
        assertThat(repository.exists("jti-2")).isFalse();
        assertThat(repository.exists("jti-3")).isFalse();
    }

    @Test
    @DisplayName("delete: refresh:{jti} 키를 삭제한다")
    void delete() {
        repository.delete("jti-1");

        verify(redisTemplate).delete("refresh:jti-1");
    }
}
