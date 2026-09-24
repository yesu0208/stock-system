package arile.toy.stocksystem.bffserver.chart.repository;

import arile.toy.stocksystem.bffserver.chart.dto.CandleData;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ChartSnapshotRepositoryTest {

    private ValueOperations<String, String> valueOps;
    private ChartSnapshotRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        repository = new ChartSnapshotRepository(redisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("chart:daily:{종목} 키의 일봉 목록을 읽는다")
    void getDaily() {
        given(valueOps.get("chart:daily:005930")).willReturn("""
                [{"date": "2026-09-23", "open": 69000, "high": 70500, "low": 68800, "close": 70000, "volume": 900},
                 {"date": "2026-09-24", "open": 70000, "high": 71500, "low": 69800, "close": 71000, "volume": 1000}]
                """);

        assertThat(repository.getDaily("005930")).containsExactly(
                new CandleData("2026-09-23", 69_000, 70_500, 68_800, 70_000, 900),
                new CandleData("2026-09-24", 70_000, 71_500, 69_800, 71_000, 1_000));
    }

    @Test
    @DisplayName("chart:minute:{종목} 키의 분봉 목록을 읽는다")
    void getMinute() {
        given(valueOps.get("chart:minute:005930")).willReturn("""
                [{"date": "2026-09-24", "time": "0931", "open": 70000, "high": 70200,
                  "low": 69900, "close": 70100, "volume": 50}]
                """);

        assertThat(repository.getMinute("005930")).containsExactly(
                new MinuteCandle("2026-09-24", "0931", 70_000, 70_200, 69_900, 70_100, 50));
    }

    @Test
    @DisplayName("스냅샷이 없으면 null을 반환한다 (장 시작 전·수집 전)")
    void notFound() {
        assertThat(repository.getDaily("005930")).isNull();
        assertThat(repository.getMinute("005930")).isNull();
    }

    @Test
    @DisplayName("저장된 JSON이 깨져 있으면 null을 반환한다")
    void brokenJson() {
        given(valueOps.get("chart:daily:005930")).willReturn("{broken");
        given(valueOps.get("chart:minute:005930")).willReturn("{broken");

        assertThat(repository.getDaily("005930")).isNull();
        assertThat(repository.getMinute("005930")).isNull();
    }
}
