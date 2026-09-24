package arile.toy.stocksystem.bffserver.external.stock.event.manager;

import arile.toy.stocksystem.bffserver.chart.event.subscriber.RedisDailyCandleEventSubscriber;
import arile.toy.stocksystem.bffserver.chart.event.subscriber.RedisMinuteCandleEventSubscriber;
import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisBidAskPriceEventSubscriber;
import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisTradePriceEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.event.RedisStockDetailEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockDetailCrawlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockRealtimeRedisSubscriptionManagerTest {

    private static final String STOCK = "005930";

    @Mock private RedisMessageListenerContainer container;
    @Mock private RedisBidAskPriceEventSubscriber bidAskSubscriber;
    @Mock private RedisTradePriceEventSubscriber tradeSubscriber;
    @Mock private RedisStockDetailEventSubscriber detailSubscriber;
    @Mock private RedisDailyCandleEventSubscriber dailyCandleSubscriber;
    @Mock private RedisMinuteCandleEventSubscriber minuteCandleSubscriber;
    @Mock private StockDetailWatchRegistry watchRegistry;
    @Mock private StockDetailCrawlService crawlService;
    @Mock private ExecutorService stockDetailCrawlExecutor;

    @InjectMocks
    private StockRealtimeRedisSubscriptionManager manager;

    /** 종목 하나에 대해 기대하는 (구독자 → 채널) 연결 5개 */
    private Map<MessageListener, ChannelTopic> expectedTopics(String stockCode) {
        Map<MessageListener, ChannelTopic> expected = new LinkedHashMap<>();
        expected.put(bidAskSubscriber, new ChannelTopic("bidask." + stockCode + ":event"));
        expected.put(tradeSubscriber, new ChannelTopic("trade." + stockCode + ":event"));
        expected.put(detailSubscriber, new ChannelTopic("stockdetail." + stockCode + ":event"));
        expected.put(dailyCandleSubscriber, new ChannelTopic("dailycandle." + stockCode + ":event"));
        expected.put(minuteCandleSubscriber, new ChannelTopic("minutecandle." + stockCode + ":event"));
        return expected;
    }

    private void verifySubscribed(String stockCode) {
        expectedTopics(stockCode).forEach((subscriber, topic) ->
                verify(container).addMessageListener(subscriber, topic));
    }

    private void verifyUnsubscribed(String stockCode) {
        expectedTopics(stockCode).forEach((subscriber, topic) ->
                verify(container).removeMessageListener(subscriber, topic));
    }

    private void verifyNotUnsubscribed() {
        verify(container, never()).removeMessageListener(any(MessageListener.class), any(Topic.class));
    }

    // ===================== 구독 =====================

    @Nested
    @DisplayName("subscribe")
    class Subscribe {

        @Test
        @DisplayName("종목의 첫 구독이면 5개 채널을 구독하고, 감시를 갱신하고, 상세 크롤링을 요청한다")
        void firstSubscriber() {
            manager.subscribe("session-A", "sub-1", STOCK);

            verifySubscribed(STOCK);
            verify(watchRegistry).heartbeat(STOCK);

            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
            verify(stockDetailCrawlExecutor).submit(task.capture());
            task.getValue().run();
            verify(crawlService).crawlAndPublish(STOCK);
        }

        @Test
        @DisplayName("이미 누군가 구독 중인 종목이면 채널을 다시 구독하지 않고 크롤링도 요청하지 않는다 (감시만 갱신)")
        void additionalSubscriber() {
            manager.subscribe("session-A", "sub-1", STOCK);
            clearInvocations(container, watchRegistry, stockDetailCrawlExecutor);

            manager.subscribe("session-B", "sub-1", STOCK);

            verifyNoInteractions(container, stockDetailCrawlExecutor);
            verify(watchRegistry).heartbeat(STOCK);
        }

        @Test
        @DisplayName("같은 구독 ID가 중복으로 오면 한 번만 센다")
        void duplicateSubscriptionId_countedOnce() {
            manager.subscribe("session-A", "sub-1", STOCK);
            manager.subscribe("session-A", "sub-1", STOCK);

            manager.unsubscribeBySubscriptionId("session-A", "sub-1");

            verifyUnsubscribed(STOCK);
        }

        @Test
        @DisplayName("종목마다 각자의 채널을 따로 구독한다")
        void differentStocks() {
            manager.subscribe("session-A", "sub-1", "005930");
            manager.subscribe("session-A", "sub-2", "000660");

            verifySubscribed("005930");
            verifySubscribed("000660");
            verify(stockDetailCrawlExecutor, times(2)).submit(any(Runnable.class));
        }
    }

    // ===================== 구독 해제 =====================

    @Nested
    @DisplayName("unsubscribeBySubscriptionId")
    class Unsubscribe {

        @Test
        @DisplayName("다른 구독자가 남아 있으면 채널 구독을 유지한다")
        void othersRemain_keepsSubscription() {
            manager.subscribe("session-A", "sub-1", STOCK);
            manager.subscribe("session-B", "sub-1", STOCK);

            manager.unsubscribeBySubscriptionId("session-A", "sub-1");

            verifyNotUnsubscribed();
        }

        @Test
        @DisplayName("마지막 구독자가 해제하면 5개 채널 구독을 모두 해제한다")
        void lastSubscriber_unsubscribesAll() {
            manager.subscribe("session-A", "sub-1", STOCK);
            manager.subscribe("session-B", "sub-1", STOCK);

            manager.unsubscribeBySubscriptionId("session-A", "sub-1");
            manager.unsubscribeBySubscriptionId("session-B", "sub-1");

            verifyUnsubscribed(STOCK);
        }

        @Test
        @DisplayName("같은 세션이 같은 종목을 두 번 구독해도, 두 구독을 모두 해제하면 채널 구독이 해제된다 (카운트 누수 방지)")
        void sameSessionTwice_bothReleased() {
            manager.subscribe("session-A", "sub-1", STOCK);
            manager.subscribe("session-A", "sub-2", STOCK);

            manager.unsubscribeBySubscriptionId("session-A", "sub-1");
            verifyNotUnsubscribed();

            manager.unsubscribeBySubscriptionId("session-A", "sub-2");
            verifyUnsubscribed(STOCK);
        }

        @Test
        @DisplayName("모르는 구독 ID면 아무것도 하지 않는다 (종목 외 채널 구독 해제 등)")
        void unknownSubscription_noop() {
            manager.unsubscribeBySubscriptionId("session-A", "unknown");

            verifyNoInteractions(container);
        }

        @Test
        @DisplayName("같은 구독을 두 번 해제해도 두 번째는 무시한다")
        void unsubscribeTwice_ignored() {
            manager.subscribe("session-A", "sub-1", STOCK);
            manager.subscribe("session-B", "sub-1", STOCK);

            manager.unsubscribeBySubscriptionId("session-A", "sub-1");
            manager.unsubscribeBySubscriptionId("session-A", "sub-1");

            verifyNotUnsubscribed();
        }
    }

    // ===================== 연결 종료 =====================

    @Nested
    @DisplayName("disconnect")
    class Disconnect {

        @Test
        @DisplayName("세션의 모든 종목 구독을 해제하고, 다른 세션이 보던 종목은 유지한다")
        void releasesSessionSubscriptionsOnly() {
            manager.subscribe("session-A", "sub-1", "005930");
            manager.subscribe("session-A", "sub-2", "000660");
            manager.subscribe("session-B", "sub-1", "000660");

            manager.disconnect("session-A");

            verifyUnsubscribed("005930");
            expectedTopics("000660").forEach((subscriber, topic) ->
                    verify(container, never()).removeMessageListener(subscriber, topic));
        }

        @Test
        @DisplayName("같은 종목을 두 번 구독한 세션이 끊겨도 채널 구독이 해제된다 (카운트 누수 방지)")
        void sameStockTwice_released() {
            manager.subscribe("session-A", "sub-1", STOCK);
            manager.subscribe("session-A", "sub-2", STOCK);

            manager.disconnect("session-A");

            verifyUnsubscribed(STOCK);
        }

        @Test
        @DisplayName("세션 ID가 다른 세션 ID의 앞부분과 같아도 그 세션의 구독만 해제한다")
        void similarSessionIds_isolated() {
            manager.subscribe("session-1", "sub-1", "005930");
            manager.subscribe("session-10", "sub-1", "000660");

            manager.disconnect("session-1");

            verifyUnsubscribed("005930");
            expectedTopics("000660").forEach((subscriber, topic) ->
                    verify(container, never()).removeMessageListener(subscriber, topic));
        }

        @Test
        @DisplayName("구독이 없는 세션이 끊기면 아무것도 하지 않는다")
        void noSubscriptions_noop() {
            manager.disconnect("session-A");

            verifyNoInteractions(container);
        }

        @Test
        @DisplayName("모두 해제된 종목을 다시 구독하면 채널을 새로 구독하고 크롤링도 다시 요청한다")
        void resubscribeAfterRelease() {
            manager.subscribe("session-A", "sub-1", STOCK);
            manager.disconnect("session-A");
            clearInvocations(container, stockDetailCrawlExecutor);

            manager.subscribe("session-B", "sub-1", STOCK);

            verifySubscribed(STOCK);
            verify(stockDetailCrawlExecutor).submit(any(Runnable.class));
        }
    }

    // ===================== 감시 갱신 =====================

    @Test
    @DisplayName("heartbeatActiveStocks: 구독 중인 종목마다 상세 정보 감시를 갱신하고, 해제된 종목은 갱신하지 않는다")
    void heartbeatActiveStocks() {
        manager.subscribe("session-A", "sub-1", "005930");
        manager.subscribe("session-A", "sub-2", "000660");
        manager.unsubscribeBySubscriptionId("session-A", "sub-2");
        clearInvocations(watchRegistry);

        manager.heartbeatActiveStocks();

        verify(watchRegistry).heartbeat("005930");
        verify(watchRegistry, never()).heartbeat("000660");
    }
}
