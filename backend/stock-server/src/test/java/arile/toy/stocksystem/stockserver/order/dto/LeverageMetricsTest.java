package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 레버리지 지표 계산 테스트")
class LeverageMetricsTest {

    @DisplayName("현물은 명목금액만 계산하고 레버리지 지표는 null이다")
    @Test
    void givenSpot_whenCalculating_thenOnlyNotionalValue() {
        LeverageMetrics metrics = LeverageMetrics.of(LeverageRatio.SPOT, 70_000, 10);

        assertThat(metrics.notionalValue()).isEqualTo(700_000L);
        assertThat(metrics.initialMargin()).isNull();
        assertThat(metrics.maintenanceMarginRate()).isNull();
        assertThat(metrics.liquidationPrice()).isNull();
    }

    @DisplayName("레버리지 비율이 없으면 현물과 같이 처리한다")
    @Test
    void givenNullLeverage_whenCalculating_thenSameAsSpot() {
        LeverageMetrics metrics = LeverageMetrics.of(null, 70_000, 10);

        assertThat(metrics).isEqualTo(LeverageMetrics.of(LeverageRatio.SPOT, 70_000, 10));
    }

    @DisplayName("레버리지는 증거금, 유지증거율(1.4), 청산가(1.4 × 대출금 / 수량)를 계산한다")
    @ParameterizedTest(name = "{0}: 증거금 {1}, 청산가 {2}")
    @CsvSource({
            "X1_5, 466667, 32667",
            "X2,   350000, 49000",
            "X2_5, 280000, 58800"
    })
    void givenLeverage_whenCalculating_thenCalculatesMetrics(
            LeverageRatio ratio, long expectedMargin, long expectedLiquidationPrice) {

        LeverageMetrics metrics = LeverageMetrics.of(ratio, 70_000, 10);

        assertThat(metrics.notionalValue()).isEqualTo(700_000L);
        assertThat(metrics.initialMargin()).isEqualTo(expectedMargin);
        assertThat(metrics.maintenanceMarginRate()).isEqualTo(1.4);
        assertThat(metrics.liquidationPrice()).isEqualTo(expectedLiquidationPrice);
    }

    @DisplayName("레버리지 수량이 0이면 청산가는 0이다 (0으로 나누지 않음)")
    @Test
    void givenZeroQuantity_whenCalculating_thenLiquidationPriceIsZero() {
        LeverageMetrics metrics = LeverageMetrics.of(LeverageRatio.X2, 70_000, 0);

        assertThat(metrics.notionalValue()).isZero();
        assertThat(metrics.initialMargin()).isZero();
        assertThat(metrics.liquidationPrice()).isZero();
    }

    @DisplayName("가격×수량이 int 범위를 넘어도 long으로 명목금액을 계산한다")
    @Test
    void givenLargeAmount_whenCalculating_thenNoOverflow() {
        LeverageMetrics metrics = LeverageMetrics.of(LeverageRatio.SPOT, 1_000_000, 10_000);

        assertThat(metrics.notionalValue()).isEqualTo(10_000_000_000L);
    }
}
