package arile.toy.stocksystem.bffserver.stockinfo.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StockDetailWatchRegistryTest {

    private static final String KEY = "stock:detail:watch";

    private ZSetOperations<String, String> zSetOps;
    private StockDetailWatchRegistry registry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        zSetOps = mock(ZSetOperations.class);
        doReturn(zSetOps).when(redisTemplate).opsForZSet();
        registry = new StockDetailWatchRegistry(redisTemplate);
    }

    @Test
    @DisplayName("heartbeat: 종목의 점수를 현재 시각으로 갱신한다")
    void heartbeat() {
        long before = System.currentTimeMillis();

        registry.heartbeat("005930");

        ArgumentCaptor<Double> score = ArgumentCaptor.forClass(Double.class);
        verify(zSetOps).add(eq(KEY), eq("005930"), score.capture());
        assertThat(score.getValue()).isBetween((double) before, (double) System.currentTimeMillis());
    }

    @Test
    @DisplayName("getActiveCodes: 최근 15초 안에 heartbeat가 있었던 종목만 조회한다")
    void activeCodes() {
        given(zSetOps.rangeByScore(eq(KEY), anyDouble(), eq(Double.MAX_VALUE))).willReturn(Set.of("005930"));

        long now = System.currentTimeMillis();
        Set<String> active = registry.getActiveCodes();

        ArgumentCaptor<Double> min = ArgumentCaptor.forClass(Double.class);
        verify(zSetOps).rangeByScore(eq(KEY), min.capture(), eq(Double.MAX_VALUE));
        assertThat(min.getValue()).isCloseTo(now - 15_000.0, within(1_000.0));
        assertThat(active).containsExactly("005930");
    }
}
