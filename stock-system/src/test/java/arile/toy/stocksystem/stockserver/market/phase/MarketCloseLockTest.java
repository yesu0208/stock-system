package arile.toy.stocksystem.stockserver.market.phase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketCloseLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MarketCloseLock marketCloseLock;

    @BeforeEach
    void setup() {
        marketCloseLock = new MarketCloseLock(redisTemplate);
    }

    @Test
    @DisplayName("락이 존재하지 않으면 acquire 시 true를 반환한다")
    void givenLockNotExists_whenAcquire_thenReturnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:market:close"), eq("LOCKED"), any(Duration.class)))
                .thenReturn(true);

        boolean acquired = marketCloseLock.acquire();

        assertTrue(acquired);
        verify(valueOperations).setIfAbsent(eq("lock:market:close"), eq("LOCKED"), any(Duration.class));
    }

    @Test
    @DisplayName("락이 이미 존재하면 acquire 시 false를 반환한다")
    void givenLockExists_whenAcquire_thenReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:market:close"), eq("LOCKED"), any(Duration.class)))
                .thenReturn(false);

        boolean acquired = marketCloseLock.acquire();

        assertFalse(acquired);
        verify(valueOperations).setIfAbsent(eq("lock:market:close"), eq("LOCKED"), any(Duration.class));
    }

    @Test
    @DisplayName("release 호출 시 Redis에서 락 키를 삭제한다")
    void whenRelease_thenDeleteKey() {
        marketCloseLock.release();

        verify(redisTemplate).delete("lock:market:close");
    }
}
