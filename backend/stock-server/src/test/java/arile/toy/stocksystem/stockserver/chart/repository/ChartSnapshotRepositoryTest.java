package arile.toy.stocksystem.stockserver.chart.repository;

import arile.toy.stocksystem.stockserver.chart.dto.CandleData;
import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Repository] 차트 스냅샷 캐시 저장 테스트")
class ChartSnapshotRepositoryTest {

    private ValueOperations<String, String> valueOps;
    private ChartSnapshotRepository sut;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        sut = new ChartSnapshotRepository(redisTemplate, new ObjectMapper());
    }

    @DisplayName("일봉·분봉을 JSON으로 12시간 TTL과 함께 저장한다")
    @Test
    void whenSaving_thenStoresJsonWithTtl() {
        sut.saveDaily("005930", List.of(new CandleData("20260925", 1, 2, 3, 4, 5)));
        sut.saveMinute("005930", List.of(new MinuteCandle("20260925", "093000", 1, 2, 3, 4, 5)));

        then(valueOps).should().set(eq("chart:daily:005930"), contains("20260925"), eq(Duration.ofHours(12)));
        then(valueOps).should().set(eq("chart:minute:005930"), contains("093000"), eq(Duration.ofHours(12)));
    }

    @DisplayName("Redis 저장이 실패해도 예외를 던지지 않는다")
    @Test
    void givenRedisFails_whenSaving_thenSwallows() {
        willThrow(new IllegalStateException("redis down"))
                .given(valueOps).set(anyString(), anyString(), any(Duration.class));

        assertThatNoException().isThrownBy(() -> {
            sut.saveDaily("005930", List.of());
            sut.saveMinute("005930", List.of());
        });
    }
}
