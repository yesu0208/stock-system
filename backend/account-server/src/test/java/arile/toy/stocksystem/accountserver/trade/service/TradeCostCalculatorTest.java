package arile.toy.stocksystem.accountserver.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TradeCostCalculatorTest {

    private final TradeCostCalculator calculator = new TradeCostCalculator();

    @ParameterizedTest(name = "금액 {0} → 수수료 {1}")
    @CsvSource({
            "1000000, 150",  // 정확히 나누어떨어짐
            "284000, 43",    // 42.6 → 올림
            "4000, 1",       // 0.6 → 올림
            "3000, 0"        // 0.45 → 내림
    })
    @DisplayName("calculateFee: 금액의 0.015%를 반올림한다")
    void calculateFee(long amount, long expected) {
        assertThat(calculator.calculateFee(amount)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "금액 {0} → 거래세 {1}")
    @CsvSource({
            "1000000, 2000", // 정확히 나누어떨어짐
            "284000, 568",
            "300, 1",        // 0.6 → 올림
            "200, 0"         // 0.4 → 내림
    })
    @DisplayName("calculateTax: 금액의 0.2%를 반올림한다")
    void calculateTax(long amount, long expected) {
        assertThat(calculator.calculateTax(amount)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "금액 {0}")
    @ValueSource(longs = {0L, -1L, -1_000_000L})
    @DisplayName("금액이 0 이하이면 수수료와 거래세 모두 0이다")
    void whenAmountNotPositive_returnsZero(long amount) {
        assertThat(calculator.calculateFee(amount)).isZero();
        assertThat(calculator.calculateTax(amount)).isZero();
    }
}
