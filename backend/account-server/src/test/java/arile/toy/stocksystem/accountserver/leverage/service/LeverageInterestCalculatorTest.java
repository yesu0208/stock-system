package arile.toy.stocksystem.accountserver.leverage.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LeverageInterestCalculatorTest {

    private final LeverageInterestCalculator calculator = new LeverageInterestCalculator();

    @ParameterizedTest(name = "대출금 {0}, {1}일 → {2}원")
    @CsvSource({
            "365000, 1, 85",
            "365000, 3, 255",       // 주말 포함 몰아서 청구
            "1000000, 1, 233",      // 232.88 → 반올림
            "1000, 1, 0"            // 0.23 → 반올림 시 0
    })
    @DisplayName("calculateInterest: 대출금 × 연 8.5% / 365 × 경과일수를 반올림한다")
    void calculateInterest(long loanAmount, long days, long expected) {
        assertThat(calculator.calculateInterest(loanAmount, days)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "대출금 {0}, {1}일")
    @CsvSource({"0, 1", "-1000, 1", "365000, 0", "365000, -1"})
    @DisplayName("calculateInterest: 대출금이나 경과일수가 0 이하이면 0이다")
    void calculateInterest_nonPositive(long loanAmount, long days) {
        assertThat(calculator.calculateInterest(loanAmount, days)).isZero();
    }

    @Test
    @DisplayName("calculateDailyInterest: 하루치 이자를 계산한다")
    void calculateDailyInterest() {
        assertThat(calculator.calculateDailyInterest(365_000L)).isEqualTo(85L);
    }
}
