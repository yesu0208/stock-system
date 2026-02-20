package arile.toy.stocksystem.bffserver.external.stock.event.manager;

import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisBidAskPriceEventSubscriber;
import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisTradePriceEventSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockRealtimeRedisSubscriptionManagerTest {

    @Mock
    private RedisMessageListenerContainer container;

    @Mock
    private RedisBidAskPriceEventSubscriber bidAskSubscriber;

    @Mock
    private RedisTradePriceEventSubscriber tradeSubscriber;

    @InjectMocks
    private StockRealtimeRedisSubscriptionManager manager;

    private final String session1 = "s1";
    private final String session2 = "s2";
    private final String stockCode1 = "005930";
    private final String stockCode2 = "000660";

    private ChannelTopic bidAskTopic(String stockCode) {
        return new ChannelTopic("bidask." + stockCode + ":event");
    }

    private ChannelTopic tradeTopic(String stockCode) {
        return new ChannelTopic("trade." + stockCode + ":event");
    }

    @Test
    @DisplayName("단일 세션 구독 시 listener 등록")
    void givenSingleSession_whenSubscribe_thenAddsListeners() {
        // given
        // (session1, stockCode1 준비는 없음, 바로 구독)

        // when
        manager.subscribe(session1, stockCode1);

        // then
        verify(container).addMessageListener(bidAskSubscriber, bidAskTopic(stockCode1));
        verify(container).addMessageListener(tradeSubscriber, tradeTopic(stockCode1));

        AtomicInteger count = manager.getStockRefCount().get(stockCode1);
        assertNotNull(count);
        assertEquals(1, count.get());

        Set<String> stocks = manager.getSessionSubscriptions().get(session1);
        assertTrue(stocks.contains(stockCode1));
    }

    @Test
    @DisplayName("동일 종목 다른 세션 추가 구독 시 listener 중복 등록 안됨")
    void givenMultipleSessions_whenSubscribe_thenIncrementsRefCountOnly() {
        // given
        manager.subscribe(session1, stockCode1);
        reset(container);

        // when
        manager.subscribe(session2, stockCode1);

        // then
        verify(container, never()).addMessageListener(any(MessageListener.class), any(ChannelTopic.class));

        AtomicInteger count = manager.getStockRefCount().get(stockCode1);
        assertEquals(2, count.get());
    }

    @Test
    @DisplayName("단일 세션 구독 해제 시 refCount 감소, listener 제거 아님")
    void givenMultipleSessions_whenUnsubscribeSingleSession_thenDecrementsRefCount() {
        // given
        manager.subscribe(session1, stockCode1);
        manager.subscribe(session2, stockCode1);
        reset(container);

        // when
        manager.unsubscribe(session1, stockCode1);

        // then
        AtomicInteger count = manager.getStockRefCount().get(stockCode1);
        assertEquals(1, count.get());

        verify(container, never()).removeMessageListener(any(MessageListener.class), any(ChannelTopic.class));
    }

    @Test
    @DisplayName("마지막 구독 해제 시 listener 제거")
    void givenSingleSession_whenUnsubscribeLastSession_thenRemovesListener() {
        // given
        manager.subscribe(session1, stockCode1);
        reset(container);

        // when
        manager.unsubscribe(session1, stockCode1);

        // then
        assertNull(manager.getStockRefCount().get(stockCode1));

        verify(container).removeMessageListener(bidAskSubscriber, bidAskTopic(stockCode1));
        verify(container).removeMessageListener(tradeSubscriber, tradeTopic(stockCode1));
    }

    @Test
    @DisplayName("unsubscribeAll 호출 시 모든 종목 구독 해제")
    void givenSubscribedSession_whenUnsubscribeAll_thenRemovesAllSubscriptions() {
        // given
        manager.subscribe(session1, stockCode1);
        manager.subscribe(session1, stockCode2);
        reset(container);

        // when
        manager.unsubscribeAll(session1);

        // then
        assertNull(manager.getSessionSubscriptions().get(session1));
        assertNull(manager.getStockRefCount().get(stockCode1));
        assertNull(manager.getStockRefCount().get(stockCode2));

        verify(container).removeMessageListener(bidAskSubscriber, bidAskTopic(stockCode1));
        verify(container).removeMessageListener(tradeSubscriber, tradeTopic(stockCode1));
        verify(container).removeMessageListener(bidAskSubscriber, bidAskTopic(stockCode2));
        verify(container).removeMessageListener(tradeSubscriber, tradeTopic(stockCode2));
    }

    @Test
    @DisplayName("disconnect 호출 시 unsubscribeAll과 동일 동작")
    void givenSubscribedSession_whenDisconnect_thenRemovesAllSubscriptions() {
        // given
        manager.subscribe(session1, stockCode1);
        manager.subscribe(session1, stockCode2);
        reset(container);

        // when
        manager.disconnect(session1);

        // then
        assertNull(manager.getSessionSubscriptions().get(session1));
        assertNull(manager.getStockRefCount().get(stockCode1));
        assertNull(manager.getStockRefCount().get(stockCode2));

        verify(container).removeMessageListener(bidAskSubscriber, bidAskTopic(stockCode1));
        verify(container).removeMessageListener(tradeSubscriber, tradeTopic(stockCode1));
        verify(container).removeMessageListener(bidAskSubscriber, bidAskTopic(stockCode2));
        verify(container).removeMessageListener(tradeSubscriber, tradeTopic(stockCode2));
    }
}
