package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySingleStockOrderQueueTest {

    private static final Instant T0 = Instant.parse("2026-09-24T00:00:00Z");

    private final InMemorySingleStockOrderQueue queue = new InMemorySingleStockOrderQueue();

    /** 주문 ID, 매수·매도, 가격, 접수 시각(T0 기준 초) */
    private static OrderDto order(long orderId, OrderType type, int price, long secondsAfterT0) {
        return new OrderDto(orderId, "user" + orderId, "005930", type, LeverageRatio.SPOT,
                price, 10, 10, OrderStatus.OPEN, T0.plusSeconds(secondsAfterT0),
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
    }

    // ===================== 체결 우선순위 =====================

    @Nested
    @DisplayName("매수 대기열")
    class Buy {

        @Test
        @DisplayName("비싸게 사려는 주문이 먼저 나온다 (가격 우선)")
        void higherPriceFirst() {
            queue.orderEnqueue(order(1, OrderType.BUY, 70_000, 0));
            queue.orderEnqueue(order(2, OrderType.BUY, 71_000, 1));
            queue.orderEnqueue(order(3, OrderType.BUY, 69_000, 2));

            assertThat(queue.pollBuy().orderId()).isEqualTo(2);
            assertThat(queue.pollBuy().orderId()).isEqualTo(1);
            assertThat(queue.pollBuy().orderId()).isEqualTo(3);
        }

        @Test
        @DisplayName("같은 가격이면 먼저 낸 주문이 먼저 나온다 (시간 우선 — 가격만 역순, 시간은 역순이 아님)")
        void samePrice_earlierFirst() {
            queue.orderEnqueue(order(1, OrderType.BUY, 70_000, 5));
            queue.orderEnqueue(order(2, OrderType.BUY, 70_000, 1));
            queue.orderEnqueue(order(3, OrderType.BUY, 70_000, 3));

            assertThat(queue.pollBuy().orderId()).isEqualTo(2);
            assertThat(queue.pollBuy().orderId()).isEqualTo(3);
            assertThat(queue.pollBuy().orderId()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("매도 대기열")
    class Sell {

        @Test
        @DisplayName("싸게 팔려는 주문이 먼저 나온다 (가격 우선)")
        void lowerPriceFirst() {
            queue.orderEnqueue(order(1, OrderType.SELL, 70_000, 0));
            queue.orderEnqueue(order(2, OrderType.SELL, 69_000, 1));
            queue.orderEnqueue(order(3, OrderType.SELL, 71_000, 2));

            assertThat(queue.pollSell().orderId()).isEqualTo(2);
            assertThat(queue.pollSell().orderId()).isEqualTo(1);
            assertThat(queue.pollSell().orderId()).isEqualTo(3);
        }

        @Test
        @DisplayName("같은 가격이면 먼저 낸 주문이 먼저 나온다 (시간 우선)")
        void samePrice_earlierFirst() {
            queue.orderEnqueue(order(1, OrderType.SELL, 70_000, 5));
            queue.orderEnqueue(order(2, OrderType.SELL, 70_000, 1));

            assertThat(queue.pollSell().orderId()).isEqualTo(2);
            assertThat(queue.pollSell().orderId()).isEqualTo(1);
        }
    }

    // ===================== 조회 =====================

    @Nested
    @DisplayName("조회·꺼내기")
    class PeekAndPoll {

        @Test
        @DisplayName("매수·매도는 서로 다른 대기열에 들어간다")
        void separateQueues() {
            queue.orderEnqueue(order(1, OrderType.BUY, 70_000, 0));
            queue.orderEnqueue(order(2, OrderType.SELL, 71_000, 0));

            assertThat(queue.peekBuy()).map(OrderDto::orderId).contains(1L);
            assertThat(queue.peekSell()).map(OrderDto::orderId).contains(2L);
        }

        @Test
        @DisplayName("peek은 꺼내지 않고 맨 앞 주문을 보여 준다")
        void peekDoesNotRemove() {
            queue.orderEnqueue(order(1, OrderType.BUY, 70_000, 0));

            assertThat(queue.peekBuy()).isPresent();
            assertThat(queue.peekBuy()).isPresent();
            assertThat(queue.pollBuy().orderId()).isEqualTo(1);
        }

        @Test
        @DisplayName("비어 있으면 peek은 빈 값, poll은 null이다")
        void empty() {
            assertThat(queue.peekBuy()).isEmpty();
            assertThat(queue.peekSell()).isEmpty();
            assertThat(queue.pollBuy()).isNull();
            assertThat(queue.pollSell()).isNull();
        }
    }

    // ===================== 삭제 =====================

    @Nested
    @DisplayName("주문 ID로 삭제 (취소)")
    class Remove {

        @Test
        @DisplayName("매수·매도 어느 쪽에 있든 찾아서 지우고 true를 돌려준다")
        void removeFromEitherSide() {
            queue.orderEnqueue(order(1, OrderType.BUY, 70_000, 0));
            queue.orderEnqueue(order(2, OrderType.SELL, 71_000, 0));

            assertThat(queue.removeByOrderId(1L)).isTrue();
            assertThat(queue.removeByOrderId(2L)).isTrue();
            assertThat(queue.peekBuy()).isEmpty();
            assertThat(queue.peekSell()).isEmpty();
        }

        @Test
        @DisplayName("없는 주문이면 false이고 다른 주문은 그대로다")
        void removeMissing() {
            queue.orderEnqueue(order(1, OrderType.BUY, 70_000, 0));

            assertThat(queue.removeByOrderId(99L)).isFalse();
            assertThat(queue.peekBuy()).isPresent();
        }

        @Test
        @DisplayName("가운데 주문을 지워도 남은 주문의 우선순위는 유지된다")
        void removeMiddle_keepsOrder() {
            queue.orderEnqueue(order(1, OrderType.BUY, 71_000, 0));
            queue.orderEnqueue(order(2, OrderType.BUY, 70_000, 0));
            queue.orderEnqueue(order(3, OrderType.BUY, 69_000, 0));

            queue.removeByOrderId(2L);

            assertThat(queue.pollBuy().orderId()).isEqualTo(1);
            assertThat(queue.pollBuy().orderId()).isEqualTo(3);
        }
    }

    // ===================== 순위 스냅샷 =====================

    @Nested
    @DisplayName("순위 스냅샷 (대기 순번 안내용)")
    class Snapshot {

        @Test
        @DisplayName("체결 우선순위대로 정렬된 목록을 돌려주고, 대기열에서 꺼내지 않는다")
        void rankedWithoutRemoving() {
            queue.orderEnqueue(order(1, OrderType.BUY, 70_000, 2));
            queue.orderEnqueue(order(2, OrderType.BUY, 71_000, 3));
            queue.orderEnqueue(order(3, OrderType.BUY, 70_000, 1));
            queue.orderEnqueue(order(4, OrderType.SELL, 72_000, 0));

            assertThat(queue.snapshotRanked(OrderType.BUY)).extracting(OrderDto::orderId).containsExactly(2L, 3L, 1L);
            assertThat(queue.snapshotRanked(OrderType.SELL)).extracting(OrderDto::orderId).containsExactly(4L);
            assertThat(queue.pollBuy().orderId()).isEqualTo(2);
        }

        @Test
        @DisplayName("비어 있으면 빈 목록이다")
        void empty() {
            assertThat(queue.snapshotRanked(OrderType.BUY)).isEmpty();
        }
    }
}
