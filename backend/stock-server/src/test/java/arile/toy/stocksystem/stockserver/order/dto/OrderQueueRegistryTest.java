package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderQueueRegistryTest {

    private static final Instant T0 = Instant.parse("2026-09-24T00:00:00Z");

    private final OrderQueueRegistry registry = new OrderQueueRegistry();

    private static OrderDto order(long orderId, String stockCode, OrderType type, int price) {
        return new OrderDto(orderId, "user" + orderId, stockCode, type, LeverageRatio.SPOT,
                price, 10, 10, OrderStatus.OPEN, T0.plusSeconds(orderId),
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
    }

    @Test
    @DisplayName("주문은 종목별 대기열에 들어가 다른 종목과 섞이지 않는다 (다른 종목끼리 체결되지 않도록)")
    void separatedByStock() {
        registry.orderEnqueue(order(1, "005930", OrderType.BUY, 70_000));
        registry.orderEnqueue(order(2, "000660", OrderType.SELL, 120_000));

        assertThat(registry.peekBuy("005930")).map(OrderDto::orderId).contains(1L);
        assertThat(registry.peekSell("005930")).isEmpty();
        assertThat(registry.peekSell("000660")).map(OrderDto::orderId).contains(2L);
        assertThat(registry.peekBuy("000660")).isEmpty();
    }

    @Test
    @DisplayName("같은 종목의 주문은 하나의 대기열에서 체결 우선순위대로 나온다")
    void sameStock_sharedQueue() {
        registry.orderEnqueue(order(1, "005930", OrderType.BUY, 70_000));
        registry.orderEnqueue(order(2, "005930", OrderType.BUY, 71_000));
        registry.orderEnqueue(order(3, "005930", OrderType.SELL, 72_000));

        assertThat(registry.pollBuy("005930").orderId()).isEqualTo(2);
        assertThat(registry.pollBuy("005930").orderId()).isEqualTo(1);
        assertThat(registry.pollSell("005930").orderId()).isEqualTo(3);
    }

    @Test
    @DisplayName("취소는 해당 종목 대기열에서만 지운다")
    void cancel_onlyThatStock() {
        registry.orderEnqueue(order(1, "005930", OrderType.BUY, 70_000));
        registry.orderEnqueue(order(2, "000660", OrderType.BUY, 120_000));

        registry.orderCancel(1L, "005930");

        assertThat(registry.peekBuy("005930")).isEmpty();
        assertThat(registry.peekBuy("000660")).isPresent();
    }

    @Test
    @DisplayName("다른 종목코드로 취소를 요청하면 지우지 않는다")
    void cancel_wrongStock_noop() {
        registry.orderEnqueue(order(1, "005930", OrderType.BUY, 70_000));

        registry.orderCancel(1L, "000660");

        assertThat(registry.peekBuy("005930")).isPresent();
    }

    @Test
    @DisplayName("순위 스냅샷은 해당 종목·방향의 주문만 우선순위대로 돌려준다")
    void snapshotRanked() {
        registry.orderEnqueue(order(1, "005930", OrderType.SELL, 72_000));
        registry.orderEnqueue(order(2, "005930", OrderType.SELL, 71_000));
        registry.orderEnqueue(order(3, "000660", OrderType.SELL, 70_000));

        assertThat(registry.snapshotRanked("005930", OrderType.SELL))
                .extracting(OrderDto::orderId)
                .containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("주문이 없던 종목을 조회하면 빈 결과다")
    void unknownStock_empty() {
        assertThat(registry.peekBuy("999999")).isEmpty();
        assertThat(registry.pollSell("999999")).isNull();
        assertThat(registry.snapshotRanked("999999", OrderType.BUY)).isEmpty();
    }
}
