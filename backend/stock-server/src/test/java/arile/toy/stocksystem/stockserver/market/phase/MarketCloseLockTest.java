package arile.toy.stocksystem.stockserver.market.phase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Lock] 장 마감 Job 그룹별 락 테스트")
@ExtendWith(MockitoExtension.class)
class MarketCloseLockTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private MarketCloseLock regularLock;
    private AfterMarketCloseLock afterLock;

    @BeforeEach
    void setUp() {
        regularLock = new MarketCloseLock(redisTemplate);
        ReflectionTestUtils.setField(regularLock, "stockGroup", "A");
        afterLock = new AfterMarketCloseLock(redisTemplate);
        ReflectionTestUtils.setField(afterLock, "stockGroup", "A");
    }

    @DisplayName("그룹별 락 키를 10분 TTL로 선점하면 true를 반환한다")
    @Test
    void givenFree_whenAcquiring_thenTrue() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent("lock:market:close:A", "LOCKED", Duration.ofMinutes(10))).willReturn(true);

        assertThat(regularLock.acquire()).isTrue();
    }

    @DisplayName("이미 다른 인스턴스가 잡은 락이면(false/null) false를 반환한다")
    @Test
    void givenTaken_whenAcquiring_thenFalse() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent("lock:market:close:A", "LOCKED", Duration.ofMinutes(10)))
                .willReturn(false)
                .willReturn(null);

        assertThat(regularLock.acquire()).isFalse();
        assertThat(regularLock.acquire()).isFalse();
    }

    @DisplayName("해제하면 그룹별 락 키를 삭제한다")
    @Test
    void whenReleasing_thenDeletesKey() {
        regularLock.release();

        then(redisTemplate).should().delete("lock:market:close:A");
    }

    @DisplayName("애프터마켓 락은 별도 키를 쓴다")
    @Test
    void givenAfterMarket_whenAcquiringAndReleasing_thenUsesSeparateKey() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent("lock:after-market:close:A", "LOCKED", Duration.ofMinutes(10))).willReturn(true);

        assertThat(afterLock.acquire()).isTrue();
        afterLock.release();

        then(redisTemplate).should().delete("lock:after-market:close:A");
    }
}
