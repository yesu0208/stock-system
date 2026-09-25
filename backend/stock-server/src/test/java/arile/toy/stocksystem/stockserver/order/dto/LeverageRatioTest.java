package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class LeverageRatioTest {

    @Test
    @DisplayName("현금(SPOT)만 isSpot이 참이다")
    void isSpot() {
        assertThat(LeverageRatio.SPOT.isSpot()).isTrue();
        assertThat(LeverageRatio.X1_5.isSpot()).isFalse();
        assertThat(LeverageRatio.X2.isSpot()).isFalse();
        assertThat(LeverageRatio.X2_5.isSpot()).isFalse();
    }

    @ParameterizedTest(name = "{0} 배율, 금액 {1} → 증거금 {2}, 대출금 {3}")
    @CsvSource({
            "SPOT,  690000, 690000,      0",
            "X1_5,  690000, 460000, 230000",
            "X2,    690000, 345000, 345000",
            "X2_5,  690000, 276000, 414000",
            "X1_5,    1000,    667,    333", // 3분의 2 = 666.67 → 반올림 667
            "X1_5,       1,      1,      0", // 0.67 -> 반올림 1
            "X2,         1,      1,      0", // 0.5 -> 반올림(half-up) 1
            "X2_5,       1,      0,      1" // 0.4 -> 반올림 0
    })
    @DisplayName("증거금은 금액 × 증거금 비율을 반올림하고, 대출금은 나머지다")
    void marginAndLoan(LeverageRatio ratio, long amount, long expectedMargin, long expectedLoan) {
        assertThat(ratio.calculateMarginDeposit(amount)).isEqualTo(expectedMargin);
        assertThat(ratio.calculateLoanAmount(amount)).isEqualTo(expectedLoan);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LeverageRatio.class)
    @DisplayName("어떤 금액이든 증거금 + 대출금 = 금액이다 (반올림으로 1원이 사라지거나 생기지 않음)")
    void marginPlusLoanEqualsAmount(LeverageRatio ratio) {
        for (long amount : new long[]{0, 1, 2, 3, 7, 999, 1_000, 69_000, 690_001, 123_456_789}) {
            assertThat(ratio.calculateMarginDeposit(amount) + ratio.calculateLoanAmount(amount))
                    .as("%s, 금액 %d", ratio, amount)
                    .isEqualTo(amount);
        }
    }

    @Test
    @DisplayName("금액이 0이면 증거금·대출금도 0이다")
    void zeroAmount() {
        for (LeverageRatio ratio : LeverageRatio.values()) {
            assertThat(ratio.calculateMarginDeposit(0)).isZero();
            assertThat(ratio.calculateLoanAmount(0)).isZero();
        }
    }
}
