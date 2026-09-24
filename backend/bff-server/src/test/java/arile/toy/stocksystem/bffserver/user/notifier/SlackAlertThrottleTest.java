package arile.toy.stocksystem.bffserver.user.notifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class SlackAlertThrottleTest {

    private static final String KEY = "slack:throttle:IllegalStateException";
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private ValueOperations<String, String> valueOps;
    private SlackAlertThrottle throttle;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        throttle = new SlackAlertThrottle(redisTemplate);
    }

    @Test
    @DisplayName("1분 안에 처음 발생한 에러 타입이면 보낸다 (60초 TTL 키 선점)")
    void firstInWindow_sends() {
        given(valueOps.setIfAbsent(KEY, "1", WINDOW)).willReturn(true);

        assertThat(throttle.shouldSend("IllegalStateException")).isTrue();
    }

    @Test
    @DisplayName("1분 안에 이미 보낸 에러 타입이면 억제한다")
    void duplicateInWindow_suppresses() {
        given(valueOps.setIfAbsent(KEY, "1", WINDOW)).willReturn(false);

        assertThat(throttle.shouldSend("IllegalStateException")).isFalse();
    }

    @Test
    @DisplayName("Redis 응답이 null이면 억제한다")
    void nullResult_suppresses() {
        given(valueOps.setIfAbsent(KEY, "1", WINDOW)).willReturn(null);

        assertThat(throttle.shouldSend("IllegalStateException")).isFalse();
    }

    @Test
    @DisplayName("Redis 장애 시에는 알림이 묻히지 않도록 보낸다")
    void redisFailure_sends() {
        given(valueOps.setIfAbsent(KEY, "1", WINDOW))
                .willThrow(new RedisConnectionFailureException("connection refused"));

        assertThat(throttle.shouldSend("IllegalStateException")).isTrue();
    }
}
