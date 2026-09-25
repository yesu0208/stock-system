package arile.toy.stocksystem.stockserver.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Util] KRX 호가 단위 계산 테스트")
class TickSizeCalculatorTest {

    @DisplayName("가격 구간별 호가 단위와 구간 시작가 (경계값 포함)")
    @ParameterizedTest(name = "{0}원 → 단위 {1}, 기준 {2}")
    @CsvSource({
            "0, 1, 0",
            "1999, 1, 0",
            "2000, 5, 2000",
            "4999, 5, 2000",
            "5000, 10, 5000",
            "19999, 10, 5000",
            "20000, 50, 20000",
            "49999, 50, 20000",
            "50000, 100, 50000",
            "199999, 100, 50000",
            "200000, 500, 200000",
            "499999, 500, 200000",
            "500000, 1000, 500000",
            "1500000, 1000, 500000"
    })
    void whenGettingTickSizeAndBaseline_thenByRange(int price, int tickSize, int baseline) {
        assertThat(TickSizeCalculator.getTickSize(price)).isEqualTo(tickSize);
        assertThat(TickSizeCalculator.getTickBaseline(price)).isEqualTo(baseline);
    }

    @DisplayName("반올림·올림·내림 (이미 호가 단위면 그대로, 음수는 0)")
    @ParameterizedTest(name = "{0}원 → 반올림 {1}, 올림 {2}, 내림 {3}")
    @CsvSource({
            "72310, 72300, 72400, 72300",
            "72350, 72400, 72400, 72300",
            "72400, 72400, 72400, 72400",
            "3002, 3000, 3005, 3000",
            "3003, 3005, 3005, 3000",
            "1234, 1234, 1234, 1234",
            "612300, 612000, 613000, 612000",
            "-100, 0, 0, 0"
    })
    void whenRounding_thenSnapsToTick(int price, int snapped, int ceiled, int floored) {
        assertThat(TickSizeCalculator.snapToTick(price)).isEqualTo(snapped);
        assertThat(TickSizeCalculator.ceilToTick(price)).isEqualTo(ceiled);
        assertThat(TickSizeCalculator.floorToTick(price)).isEqualTo(floored);
    }
}
