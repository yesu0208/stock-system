package arile.toy.stocksystem.accountserver.leverage.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MarginRatioCalculatorTest {

    private final MarginRatioCalculator calculator = new MarginRatioCalculator();

    @Test
    @DisplayName("담보비율 = 평가금액 / 대출금 × 100")
    void calculateRatio() {
        assertThat(calculator.calculateRatio(490_000L, 350_000L)).isEqualTo(140.0);
        assertThat(calculator.calculateRatio(700_000L, 350_000L)).isEqualTo(200.0);
    }

    @ParameterizedTest(name = "대출금 {0}")
    @ValueSource(longs = {0L, -1L})
    @DisplayName("대출금이 0 이하이면 위험 없음으로 보고 Double.MAX_VALUE를 반환한다")
    void calculateRatio_withoutLoan(long loanAmount) {
        assertThat(calculator.calculateRatio(490_000L, loanAmount)).isEqualTo(Double.MAX_VALUE);
    }

    @ParameterizedTest(name = "담보비율 {0} → 미달={1}")
    @CsvSource({"139.99, true", "140.0, false", "200.0, false"})
    @DisplayName("최소유지 담보비율(140%) 미만이면 미달이다")
    void isBelowMaintenance(double ratio, boolean expected) {
        assertThat(calculator.isBelowMaintenance(ratio)).isEqualTo(expected);
    }
}
