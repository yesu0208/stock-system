package arile.toy.stocksystem.stockserver.trailingstop.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Calculator] 트레일링 스탑 발동가 계산 테스트")
class TrailingStopPriceCalculatorTest {

    @DisplayName("매수는 기준가 × (1 + 비율)을 호가 단위로 올림한다 (반등 매수: 이 가격 이상에서 발동)")
    @ParameterizedTest(name = "기준가 {0}, {1}% → {2}")
    @CsvSource({
            "70000, 3.0, 72100",
            "70000, 2.5, 71800",   // 71,750 → 71,800
            "68000, 3.0, 70100",   // 70,040 → 70,100
            "49000, 2.0, 50000",   // 49,980 → 50,000 (50원 단위 구간에서 올림)
            "70000, 0.0, 70000"
    })
    void givenBuy_whenCalculating_thenCeilsToTick(int basePrice, double percent, int expected) {
        assertThat(TrailingStopPriceCalculator.calcTrigger(TrailingStopType.BUY, basePrice, percent))
                .isEqualTo(expected);
    }

    @DisplayName("매도는 기준가 × (1 - 비율)을 호가 단위로 내림한다 (손절 매도: 이 가격 이하에서 발동)")
    @ParameterizedTest(name = "기준가 {0}, {1}% → {2}")
    @CsvSource({
            "70000, 3.0, 67900",
            "70000, 2.5, 68200",   // 68,250 → 68,200
            "72000, 3.0, 69800",   // 69,840 → 69,800
            "70000, 0.0, 70000"
    })
    void givenSell_whenCalculating_thenFloorsToTick(int basePrice, double percent, int expected) {
        assertThat(TrailingStopPriceCalculator.calcTrigger(TrailingStopType.SELL, basePrice, percent))
                .isEqualTo(expected);
    }
}
