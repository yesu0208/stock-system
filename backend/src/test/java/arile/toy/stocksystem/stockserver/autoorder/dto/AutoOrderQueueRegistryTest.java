package arile.toy.stocksystem.stockserver.autoorder.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

class AutoOrderQueueRegistryTest {

    private AutoOrderQueueRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AutoOrderQueueRegistry();
    }

    @Test
    @DisplayName("AutoOrder 등록 후 BUY/SELL 우선순위 확인")
    void givenBuyAndSellOrders_whenEnqueue_thenPeekShowsCorrectPriority() {
        // given
        Instant now = Instant.now();
        AutoOrderDto buyOrder = new AutoOrderDto(
                1L, "alice", "005930", AutoOrderType.BUY,
                100, 105, 10, now
        );
        AutoOrderDto sellOrder = new AutoOrderDto(
                2L, "bob", "005930", AutoOrderType.SELL,
                120, 115, 5, now.plusSeconds(1)
        );

        // when
        registry.autoOrderEnqueue(buyOrder);
        registry.autoOrderEnqueue(sellOrder);

        // then
        AutoOrderDto buyPeek = registry.peekBuy("005930").orElseThrow();
        assertEquals(1L, buyPeek.autoOrderId());

        AutoOrderDto sellPeek = registry.peekSell("005930").orElseThrow();
        assertEquals(2L, sellPeek.autoOrderId());
    }

    @Test
    @DisplayName("BUY 주문 우선순위에 따라 Poll 시 올바른 순서 반환")
    void givenMultipleBuyOrders_whenPollBuy_thenReturnsInPriorityOrder() {
        // given
        Instant now = Instant.now();
        AutoOrderDto buyOrder1 =
                new AutoOrderDto(1L, "alice", "005930", AutoOrderType.BUY,
                        100, 105, 10, now);
        AutoOrderDto buyOrder2 =
                new AutoOrderDto(2L, "bob", "005930", AutoOrderType.BUY,
                110, 110, 5, now.plusMillis(1));

        registry.autoOrderEnqueue(buyOrder1);
        registry.autoOrderEnqueue(buyOrder2);

        // when
        AutoOrderDto firstBuy = registry.pollBuy("005930");
        AutoOrderDto secondBuy = registry.pollBuy("005930");
        AutoOrderDto noBuy = registry.pollBuy("005930");

        // then
        assertEquals(1L, firstBuy.autoOrderId());
        assertEquals(2L, secondBuy.autoOrderId());
        assertNull(noBuy);
    }

    @Test
    @DisplayName("AutoOrder 취소 시 주문 제거 및 없는 주문 취소 시 안전 처리")
    void givenExistingAndNonExistingOrder_whenCancel_thenQueueUpdatedCorrectly() {
        // given
        Instant now = Instant.now();
        AutoOrderDto buyOrder =
                new AutoOrderDto(1L, "alice", "005930", AutoOrderType.BUY,
                        100, 105, 10, now);
        registry.autoOrderEnqueue(buyOrder);

        // when & then
        assertTrue(registry.peekBuy("005930").isPresent());

        registry.autoOrderCancel(1L, "005930");
        assertFalse(registry.peekBuy("005930").isPresent());

        // 없는 주문 취소 시 에러 없이 통과
        registry.autoOrderCancel(999L, "005930");
    }

    @Test
    @DisplayName("새로운 stockCode 주문 시 자동으로 큐 생성")
    void givenNewStockCode_whenEnqueue_thenQueueCreatedAutomatically() {
        // given
        assertFalse(registry.peekBuy("000660").isPresent());
        Instant now = Instant.now();
        AutoOrderDto order =
                new AutoOrderDto(1L, "charlie", "000660", AutoOrderType.BUY,
                        150, 155, 20, now);

        // when
        registry.autoOrderEnqueue(order);

        // then
        AutoOrderDto peeked = registry.peekBuy("000660").orElseThrow();
        assertEquals(1L, peeked.autoOrderId());
    }
}
