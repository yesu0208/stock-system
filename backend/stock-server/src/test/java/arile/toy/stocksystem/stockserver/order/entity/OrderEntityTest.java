package arile.toy.stocksystem.stockserver.order.entity;

import arile.toy.stocksystem.stockserver.order.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Entity] 주문 예약 수수료·증거금 소진 테스트")
class OrderEntityTest {

    @Nested
    @DisplayName("consumeReservedFee")
    class Fee {

        @DisplayName("남은 수량을 모두 체결하면 남은 예약 수수료 전부를 소진한다")
        @Test
        void givenFullFill_whenConsuming_thenConsumesAll() {
            OrderEntity order = order(10, 105L, null);

            assertThat(order.consumeReservedFee(10)).isEqualTo(105L);
            assertThat(order.getRemainingReservedFee()).isZero();
        }

        @DisplayName("부분 체결은 수량 비율만큼 반올림해 소진하고, 여러 번 나눠 체결해도 합계가 예약액과 같다")
        @Test
        void givenPartialFills_whenConsuming_thenSumsToReserved() {
            OrderEntity order = order(10, 105L, null);

            long first = order.consumeReservedFee(3);   // 105 × 3/10 = 31.5 → 32
            order.setRemainingQuantity(7);
            long second = order.consumeReservedFee(3);  // 73 × 3/7 = 31.28 → 31
            order.setRemainingQuantity(4);
            long last = order.consumeReservedFee(4);    // 남은 42 전부

            assertThat(first).isEqualTo(32L);
            assertThat(second).isEqualTo(31L);
            assertThat(last).isEqualTo(42L);
            assertThat(first + second + last).isEqualTo(105L);
            assertThat(order.getRemainingReservedFee()).isZero();
        }

        @DisplayName("남은 수량이 없거나 0 이하이거나 예약 수수료가 없으면(매도) 0을 돌려준다")
        @Test
        void givenNothingToConsume_whenConsuming_thenZero() {
            assertThat(order(null, 105L, null).consumeReservedFee(1)).isZero();
            assertThat(order(0, 105L, null).consumeReservedFee(1)).isZero();
            assertThat(order(-1, 105L, null).consumeReservedFee(1)).isZero();
            assertThat(order(10, null, null).consumeReservedFee(1)).isZero();
        }
    }

    @Nested
    @DisplayName("consumeReservedMargin")
    class Margin {

        @DisplayName("남은 수량을 모두 체결하면 남은 증거금 전부를 소진한다")
        @Test
        void givenFullFill_whenConsuming_thenConsumesAll() {
            OrderEntity order = order(10, 105L, 350_000L);

            assertThat(order.consumeReservedMargin(10)).isEqualTo(350_000L);
            assertThat(order.getRemainingReservedMargin()).isZero();
        }

        @DisplayName("부분 체결은 수량 비율만큼 반올림해 소진하고, 여러 번 나눠 체결해도 합계가 예약 증거금과 같다")
        @Test
        void givenPartialFills_whenConsuming_thenSumsToReserved() {
            OrderEntity order = order(3, null, 100_000L);

            long first = order.consumeReservedMargin(1);   // 100,000 / 3 = 33,333.3 → 33,333
            order.setRemainingQuantity(2);
            long last = order.consumeReservedMargin(2);    // 남은 66,667 전부

            assertThat(first).isEqualTo(33_333L);
            assertThat(last).isEqualTo(66_667L);
            assertThat(order.getRemainingReservedMargin()).isZero();
        }

        @DisplayName("남은 수량이 없거나 0 이하이거나 증거금이 없으면(현물) 0을 돌려준다")
        @Test
        void givenNothingToConsume_whenConsuming_thenZero() {
            assertThat(order(null, null, 350_000L).consumeReservedMargin(1)).isZero();
            assertThat(order(0, null, 350_000L).consumeReservedMargin(1)).isZero();
            assertThat(order(-1, null, 350_000L).consumeReservedMargin(1)).isZero();
            assertThat(order(10, null, null).consumeReservedMargin(1)).isZero();
        }
    }

    private OrderEntity order(Integer remainingQuantity, Long reservedFee, Long reservedMargin) {
        return OrderEntity.of("user", "005930", OrderType.BUY,
                reservedMargin == null ? LeverageRatio.SPOT : LeverageRatio.X2,
                70_000, 10, OrderStatus.OPEN, remainingQuantity, OrderExecutionType.LIMIT,
                OrderOrigin.MANUAL, null, reservedFee, reservedMargin);
    }
}
