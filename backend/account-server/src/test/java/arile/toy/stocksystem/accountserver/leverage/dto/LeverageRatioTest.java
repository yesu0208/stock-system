package arile.toy.stocksystem.accountserver.leverage.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LeverageRatioTest {

    @ParameterizedTest(name = "{0}: 배율={1}, 증거금률={2}")
    @CsvSource({
            "SPOT, 1.0, 1.0",
            "X2, 2.0, 0.5",
            "X2_5, 2.5, 0.4"
    })
    @DisplayName("배율과 증거금률")
    void ratioAndMarginRate(LeverageRatio leverageRatio, double ratio, double marginRate) {
        assertThat(leverageRatio.getRatio()).isEqualTo(ratio);
        assertThat(leverageRatio.getMarginRate()).isEqualTo(marginRate);
    }

    @ParameterizedTest(name = "{0}: spot={1}, gold={2}, platinum={3}")
    @CsvSource({
            "SPOT, true, false, false",
            "X1_5, false, false, false",
            "X2, false, true, false",
            "X2_5, false, false, true"
    })
    @DisplayName("현물 여부와 배율별 요구 등급")
    void flags(LeverageRatio leverageRatio, boolean spot, boolean gold, boolean platinum) {
        assertThat(leverageRatio.isSpot()).isEqualTo(spot);
        assertThat(leverageRatio.requiresGold()).isEqualTo(gold);
        assertThat(leverageRatio.requiresPlatinum()).isEqualTo(platinum);
    }

    @ParameterizedTest(name = "{0} × {1} → 증거금 {2}, 대출금 {3}")
    @CsvSource({
            "SPOT, 1000000, 1000000, 0",
            "X1_5, 300000, 200000, 100000",
            "X1_5, 1000000, 666667, 333333",   // 666,666.67 → 반올림
            "X2, 700000, 350000, 350000",
            "X2_5, 1000000, 400000, 600000"
    })
    @DisplayName("증거금은 매수금액 × 증거금률을 반올림하고, 대출금은 나머지다")
    void marginDepositAndLoan(LeverageRatio leverageRatio, long purchaseAmount, long deposit, long loan) {
        assertThat(leverageRatio.calculateMarginDeposit(purchaseAmount)).isEqualTo(deposit);
        assertThat(leverageRatio.calculateLoanAmount(purchaseAmount)).isEqualTo(loan);
    }
}
