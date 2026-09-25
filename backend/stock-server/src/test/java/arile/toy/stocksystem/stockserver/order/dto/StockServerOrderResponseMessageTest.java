package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 주문 응답 메시지 생성 테스트")
class StockServerOrderResponseMessageTest {

    private static final Instant ORDER_TIME = Instant.parse("2026-09-25T00:00:00Z");

    @DisplayName("현물 주문은 명목금액만 계산하고 레버리지 지표는 null이다")
    @Test
    void givenSpot_whenCreating_thenOnlyNotionalValue() {
        var message = create(LeverageRatio.SPOT, 70_000, 10);

        assertThat(message.notionalValue()).isEqualTo(700_000L);
        assertThat(message.initialMargin()).isNull();
        assertThat(message.maintenanceMarginRate()).isNull();
        assertThat(message.liquidationPrice()).isNull();
    }

    @DisplayName("레버리지 비율이 없으면 현물처럼 레버리지 지표가 null이다")
    @Test
    void givenNullLeverage_whenCreating_thenNoLeverageMetrics() {
        var message = create(null, 70_000, 10);

        assertThat(message.notionalValue()).isEqualTo(700_000L);
        assertThat(message.initialMargin()).isNull();
        assertThat(message.maintenanceMarginRate()).isNull();
        assertThat(message.liquidationPrice()).isNull();
    }

    @DisplayName("레버리지 주문은 증거금, 유지증거율(1.4), 청산가(1.4 × 대출금 / 수량)를 계산한다")
    @ParameterizedTest(name = "{0}: 증거금 {1}, 청산가 {2}")
    @CsvSource({
            "X1_5, 466667, 32667",
            "X2,   350000, 49000",
            "X2_5, 280000, 58800"
    })
    void givenLeverage_whenCreating_thenCalculatesMetrics(
            LeverageRatio ratio, long expectedMargin, long expectedLiquidationPrice) {

        var message = create(ratio, 70_000, 10);

        assertThat(message.notionalValue()).isEqualTo(700_000L);
        assertThat(message.initialMargin()).isEqualTo(expectedMargin);
        assertThat(message.maintenanceMarginRate()).isEqualTo(1.4);
        assertThat(message.liquidationPrice()).isEqualTo(expectedLiquidationPrice);
    }

    @DisplayName("레버리지 주문 수량이 0이면 청산가는 0이다 (0으로 나누지 않음)")
    @Test
    void givenLeverageWithZeroQuantity_whenCreating_thenLiquidationPriceIsZero() {
        var message = create(LeverageRatio.X2, 70_000, 0);

        assertThat(message.notionalValue()).isZero();
        assertThat(message.initialMargin()).isZero();
        assertThat(message.liquidationPrice()).isZero();
    }

    @DisplayName("가격×수량이 int 범위를 넘어도 long으로 명목금액을 계산한다")
    @Test
    void givenLargeAmount_whenCreating_thenNoOverflow() {
        var message = create(LeverageRatio.SPOT, 1_000_000, 10_000);

        assertThat(message.notionalValue()).isEqualTo(10_000_000_000L);
    }

    @DisplayName("입력받은 주문 정보를 그대로 담는다")
    @Test
    void givenOrderInfo_whenCreating_thenCopiesFields() {
        var message = StockServerOrderResponseMessage.of(
                1L, "user", "005930", OrderType.SELL, LeverageRatio.SPOT,
                70_000, 10, 7, ORDER_TIME, OrderExecutionType.MARKET,
                OrderOrigin.TRAILING_STOP, 99L);

        assertThat(message.orderId()).isEqualTo(1L);
        assertThat(message.username()).isEqualTo("user");
        assertThat(message.stockCode()).isEqualTo("005930");
        assertThat(message.orderType()).isEqualTo(OrderType.SELL);
        assertThat(message.orderPrice()).isEqualTo(70_000);
        assertThat(message.orderQuantity()).isEqualTo(10);
        assertThat(message.remainingQuantity()).isEqualTo(7);
        assertThat(message.orderTime()).isEqualTo(ORDER_TIME);
        assertThat(message.orderExecutionType()).isEqualTo(OrderExecutionType.MARKET);
        assertThat(message.origin()).isEqualTo(OrderOrigin.TRAILING_STOP);
        assertThat(message.originId()).isEqualTo(99L);
    }

    private StockServerOrderResponseMessage create(LeverageRatio ratio, int price, int quantity) {
        return StockServerOrderResponseMessage.of(
                1L, "user", "005930", OrderType.BUY, ratio,
                price, quantity, quantity, ORDER_TIME, OrderExecutionType.LIMIT,
                OrderOrigin.MANUAL, null);
    }
}
