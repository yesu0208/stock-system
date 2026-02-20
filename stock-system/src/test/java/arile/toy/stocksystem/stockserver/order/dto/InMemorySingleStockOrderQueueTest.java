package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySingleStockOrderQueueTest {

    private InMemorySingleStockOrderQueue queue;

    @BeforeEach
    void setUp() {
        queue = new InMemorySingleStockOrderQueue();
    }

    @Test
    @DisplayName("BUY/SELL 주문을 큐에 넣고 peek 시 가격 우선순위로 조회된다")
    void givenOrders_whenEnqueueAndPeek_thenReturnsHighestBuyAndLowestSell() {
        // given
        String username = "user";
        OrderDto buy1 = new OrderDto(1L, username, "005930", OrderType.BUY,
                100, 1000, 1000, OrderStatus.OPEN, Instant.now());
        OrderDto buy2 = new OrderDto(2L, username, "005930", OrderType.BUY,
                110, 1001, 1001, OrderStatus.OPEN, Instant.now());
        OrderDto sell1 = new OrderDto(3L, username, "005930", OrderType.SELL,
                120, 1002, 1002, OrderStatus.OPEN, Instant.now());

        // when
        queue.orderEnqueue(buy1);
        queue.orderEnqueue(buy2);
        queue.orderEnqueue(sell1);

        // then
        OrderDto buy = queue.peekBuy().orElseThrow();
        assertEquals(2L, buy.orderId());

        OrderDto sell = queue.peekSell().orElseThrow();
        assertEquals(3L, sell.orderId());
    }

    @Test
    @DisplayName("BUY 주문을 poll하면 가격 높은 순으로 꺼내지고 빈 경우 null 반환")
    void givenBuyOrders_whenPollBuy_thenReturnsInPriceOrder() {
        // given
        String username = "user";
        OrderDto buy1 = new OrderDto(1L, username, "005930", OrderType.BUY,
                100, 1000, 1000, OrderStatus.OPEN, Instant.now());
        OrderDto buy2 = new OrderDto(2L, username, "005930", OrderType.BUY,
                110, 1001, 1001, OrderStatus.OPEN, Instant.now());

        queue.orderEnqueue(buy1);
        queue.orderEnqueue(buy2);

        // when & then
        assertEquals(2L, queue.pollBuy().orderId());
        assertEquals(1L, queue.pollBuy().orderId());
        assertNull(queue.pollBuy());
    }

    @Test
    @DisplayName("주문 ID로 제거하면 해당 주문이 큐에서 삭제된다")
    void givenOrders_whenRemoveByOrderId_thenRemovesCorrectly() {
        // given
        String username = "user";
        OrderDto buy1 = new OrderDto(1L, username, "005930", OrderType.BUY,
                100, 1000, 1000, OrderStatus.OPEN, Instant.now());
        OrderDto sell1 = new OrderDto(2L, username, "005930", OrderType.SELL,
                120, 1002, 1002, OrderStatus.OPEN, Instant.now());

        queue.orderEnqueue(buy1);
        queue.orderEnqueue(sell1);

        // when & then
        assertTrue(queue.removeByOrderId(1L));
        assertFalse(queue.peekBuy().isPresent());

        assertTrue(queue.removeByOrderId(2L));
        assertFalse(queue.peekSell().isPresent());

        assertFalse(queue.removeByOrderId(999L));
    }

    @Test
    @DisplayName("같은 가격의 BUY 주문은 시간 순서대로 우선순위가 결정된다")
    void givenBuyOrdersWithSamePrice_whenEnqueue_thenEarlierOrderHasPriority() {
        // given
        String username = "user";
        Instant now = Instant.now();
        Instant oneMsEarlier = now.minusMillis(1);
        OrderDto buy1 = new OrderDto(1L, username, "005930", OrderType.BUY,
                100, 1000, 1000, OrderStatus.OPEN, now);
        OrderDto buy2 = new OrderDto(2L, username, "005930", OrderType.BUY,
                110, 1001, 1001, OrderStatus.OPEN, oneMsEarlier);

        // when
        queue.orderEnqueue(buy1);
        queue.orderEnqueue(buy2);

        // then
        OrderDto buy = queue.peekBuy().orElseThrow();
        assertEquals(2L, buy.orderId());
    }
}
