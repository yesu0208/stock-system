package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Service] 주문 예약금·수수료·세금 계산 테스트")
class ReserveAmountCalculatorTest {

    private final ReserveAmountCalculator calculator = new ReserveAmountCalculator();

    @DisplayName("수수료는 주문금액의 0.015%를 반올림한 값이다")
    @ParameterizedTest(name = "주문금액 {0} → 수수료 {1}")
    @CsvSource({
            "1000000, 150",
            "3333,    0",   // 0.49995 → 0
            "3334,    1",   // 0.5001  → 1
            "6667,    1",   // 1.00005 → 1
            "1,       0"
    })
    void givenAmount_whenCalculatingFee_thenReturnsRoundedFee(long amount, long expected) {
        assertThat(calculator.calculateFee(amount)).isEqualTo(expected);
    }

    @DisplayName("세금은 주문금액의 0.2%를 반올림한 값이다")
    @ParameterizedTest(name = "주문금액 {0} → 세금 {1}")
    @CsvSource({
            "1000000, 2000",
            "284000,  568",
            "249,     0",   // 0.498 → 0
            "251,     1",   // 0.502 → 1
            "3333,    7"    // 6.666 → 7
    })
    void givenAmount_whenCalculatingTax_thenReturnsRoundedTax(long amount, long expected) {
        assertThat(calculator.calculateTax(amount)).isEqualTo(expected);
    }

    @DisplayName("주문금액이 0 이하이면 수수료와 세금은 0이다")
    @ParameterizedTest(name = "주문금액 {0}")
    @ValueSource(longs = {0L, -1L, -1000000L})
    void givenNonPositiveAmount_whenCalculating_thenReturnsZero(long amount) {
        assertThat(calculator.calculateFee(amount)).isZero();
        assertThat(calculator.calculateTax(amount)).isZero();
    }

    @DisplayName("현물 주문의 예약금은 주문금액 전체에 수수료를 더한 값이다")
    @Test
    void givenSpot_whenCalculatingReserveAmount_thenReturnsFullAmountPlusFee() {
        long reserve = calculator.calculateReserveAmount(LeverageRatio.SPOT, 1_000_000L);

        assertThat(reserve).isEqualTo(1_000_000L + 150L);
    }

    @DisplayName("레버리지 주문의 예약금은 증거금에 주문금액 기준 수수료를 더한 값이다")
    @ParameterizedTest(name = "{0}: 주문금액 {1} → 예약금 {2}")
    @CsvSource({
            "X1_5, 1000000, 666817",  // 증거금 666,667 + 수수료 150
            "X2,   1000000, 500150",  // 증거금 500,000 + 수수료 150
            "X2_5, 1000000, 400150"   // 증거금 400,000 + 수수료 150
    })
    void givenLeverage_whenCalculatingReserveAmount_thenReturnsMarginPlusFee(
            LeverageRatio ratio, long orderAmount, long expected) {

        assertThat(calculator.calculateReserveAmount(ratio, orderAmount)).isEqualTo(expected);
    }

    @DisplayName("레버리지 주문이어도 수수료는 증거금이 아닌 주문금액 전체를 기준으로 계산한다")
    @Test
    void givenLeverage_whenCalculatingReserveAmount_thenFeeIsBasedOnOrderAmount() {
        long orderAmount = 1_000_000L;
        long margin = LeverageRatio.X2.calculateMarginDeposit(orderAmount);

        long reserve = calculator.calculateReserveAmount(LeverageRatio.X2, orderAmount);

        assertThat(reserve - margin).isEqualTo(calculator.calculateFee(orderAmount));
    }
}
