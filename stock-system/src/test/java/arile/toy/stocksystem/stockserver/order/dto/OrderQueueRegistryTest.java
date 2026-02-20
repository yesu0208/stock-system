package arile.toy.stocksystem.stockserver.order.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OrderQueueRegistryTest {

    private OrderQueueRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new OrderQueueRegistry();
    }

    @Test
    @DisplayName("주문을 큐에 넣고 peek 시 BUY/SELL 우선순위가 올바르게 조회된다")
    void givenBuyAndSellOrders_whenEnqueueAndPeek_thenCorrectPriority() {
        // given
        Instant now = Instant.now();
        OrderDto buyOrder = new OrderDto(
                1L, "user1", "005930", OrderType.BUY,
                105, 10, 10, OrderStatus.OPEN, now
        );
        OrderDto sellOrder = new OrderDto(
                2L, "user2", "005930", OrderType.SELL,
                115, 5, 5, OrderStatus.OPEN, now.plusSeconds(1)
        );

        // when
        registry.orderEnqueue(buyOrder);
        registry.orderEnqueue(sellOrder);

        // then
        OrderDto buyPeek = registry.peekBuy("005930").orElseThrow();
        assertEquals(1L, buyPeek.orderId());

        OrderDto sellPeek = registry.peekSell("005930").orElseThrow();
        assertEquals(2L, sellPeek.orderId());
    }

    @Test
    @DisplayName("BUY 주문을 poll 시 가격 높은 순으로 꺼내고, 빈 경우 null 반환")
    void givenBuyOrders_whenPollBuy_thenReturnsInPriceOrder() {
        // given
        Instant now = Instant.now();
        OrderDto buy1 = new OrderDto(1L, "user1", "005930", OrderType.BUY,
                100, 10, 10, OrderStatus.OPEN, now);
        OrderDto buy2 = new OrderDto(2L, "user2", "005930", OrderType.BUY,
                110, 5, 5, OrderStatus.OPEN, now.plusMillis(1));

        registry.orderEnqueue(buy1);
        registry.orderEnqueue(buy2);

        // when
        OrderDto firstBuy = registry.pollBuy("005930");
        OrderDto secondBuy = registry.pollBuy("005930");
        OrderDto noBuy = registry.pollBuy("005930");

        // then
        assertEquals(2L, firstBuy.orderId());
        assertEquals(1L, secondBuy.orderId());
        assertNull(noBuy);
    }

    @Test
    @DisplayName("주문 ID로 주문을 취소하면 큐에서 제거된다")
    void givenExistingAndNonExistingOrders_whenOrderCancel_thenQueueUpdated() {
        // given
        Instant now = Instant.now();
        OrderDto buyOrder = new OrderDto(1L, "user1", "005930", OrderType.BUY,
                105, 10, 10, OrderStatus.OPEN, now);
        registry.orderEnqueue(buyOrder);
        assertTrue(registry.peekBuy("005930").isPresent());

        // when
        registry.orderCancel(1L, "005930");
        registry.orderCancel(999L, "005930"); // 없는 주문도 통과

        // then
        assertFalse(registry.peekBuy("005930").isPresent());
    }

    @Test
    @DisplayName("새로운 stockCode 주문 큐가 자동으로 생성되고 주문이 추가된다")
    void givenNewStockCode_whenEnqueue_thenQueueCreatedAndPeekable() {
        // given
        assertFalse(registry.peekBuy("006660").isPresent());
        Instant now = Instant.now();
        OrderDto order = new OrderDto(1L, "user1", "006660", OrderType.BUY,
                150, 20, 20, OrderStatus.OPEN, now);

        // when
        registry.orderEnqueue(order);

        // then
        OrderDto peeked = registry.peekBuy("006660").orElseThrow();
        assertEquals(1L, peeked.orderId());
    }
}
