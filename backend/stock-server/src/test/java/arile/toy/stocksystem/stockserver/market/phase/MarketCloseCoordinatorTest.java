package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.sharding.StockGroupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Coordinator] 장 마감 그룹 완료 집계 테스트")
@ExtendWith(MockitoExtension.class)
class MarketCloseCoordinatorTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private StockGroupProperties stockGroupProperties;

    private final String regularKey = "market:close:done-count:" + LocalDate.now();
    private final String afterKey = "after-market:close:done-count:" + LocalDate.now();

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        // 거래 그룹 A, B + 마감 전용 그룹 CLOSE -> 거래 그룹 수 2
        // (증가 결과가 null이면 그룹 수를 조회하지 않으므로 lenient)
        lenient().when(stockGroupProperties.getGroups()).thenReturn(Map.of(
                "A", List.of("005930"), "B", List.of("000660"), "CLOSE", List.of()));
    }
    @DisplayName("완료 그룹 수가 거래 그룹 수(CLOSE 제외)에 못 미치면 마지막이 아니다")
    @Test
    void givenNotAllDone_whenMarking_thenNotLast() {
        given(valueOps.increment(regularKey)).willReturn(1L);

        boolean last = new MarketCloseCoordinator(redisTemplate, stockGroupProperties).markDoneAndCheckLast();

        assertThat(last).isFalse();
        then(redisTemplate).should().expire(regularKey, Duration.ofHours(2));
    }

    @DisplayName("완료 그룹 수가 거래 그룹 수에 도달하면 마지막 그룹이다")
    @Test
    void givenAllDone_whenMarking_thenLast() {
        given(valueOps.increment(regularKey)).willReturn(2L);

        boolean last = new MarketCloseCoordinator(redisTemplate, stockGroupProperties).markDoneAndCheckLast();

        assertThat(last).isTrue();
    }

    @DisplayName("증가 결과가 null이면 마지막이 아니다")
    @Test
    void givenNullCount_whenMarking_thenNotLast() {
        given(valueOps.increment(regularKey)).willReturn(null);

        boolean last = new MarketCloseCoordinator(redisTemplate, stockGroupProperties).markDoneAndCheckLast();

        assertThat(last).isFalse();
    }

    @DisplayName("애프터마켓 코디네이터는 별도 키로 집계한다")
    @Test
    void givenAfterMarket_whenMarking_thenUsesSeparateKey() {
        given(valueOps.increment(afterKey)).willReturn(2L);

        boolean last = new AfterMarketCloseCoordinator(redisTemplate, stockGroupProperties).markDoneAndCheckLast();

        assertThat(last).isTrue();
        then(redisTemplate).should().expire(afterKey, Duration.ofHours(2));
    }
}
