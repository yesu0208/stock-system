package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.event.subscriber.RedisAccountUpdateEventSubscriber;
import arile.toy.stocksystem.bffserver.alert.event.subscriber.RedisAlertFiredEventSubscriber;
import arile.toy.stocksystem.bffserver.alert.event.subscriber.RedisAlertResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.alertcancel.event.subscriber.RedisAlertCancelResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.autocancel.event.subscriber.RedisAutoCancelResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.autoorder.event.subscriber.RedisAutoOrderResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.cancel.event.subscriber.RedisCancelResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.leverage.event.subscriber.RedisLiquidationEventSubscriber;
import arile.toy.stocksystem.bffserver.leverage.event.subscriber.RedisMarginCallEventSubscriber;
import arile.toy.stocksystem.bffserver.order.event.subscriber.RedisOrderResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.order.event.subscriber.RedisQueuePositionEventSubscriber;
import arile.toy.stocksystem.bffserver.otoco.event.subscriber.RedisOtocoResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.otococancel.event.subscriber.RedisOtocoCancelResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.trade.event.subscriber.RedisTradeResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.trailingstop.event.subscriber.RedisTrailingStopResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.subscriber.RedisTrailingStopCancelResponseEventSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserRedisSubscriptionRegistryTest {

    @Mock private RedisMessageListenerContainer container;

    @Mock private RedisOrderResponseEventSubscriber orderSubscriber;
    @Mock private RedisAutoOrderResponseEventSubscriber autoOrderSubscriber;
    @Mock private RedisTradeResponseEventSubscriber tradeSubscriber;
    @Mock private RedisCancelResponseEventSubscriber cancelSubscriber;
    @Mock private RedisAutoCancelResponseEventSubscriber autoCancelSubscriber;
    @Mock private RedisAccountUpdateEventSubscriber accountSubscriber;
    @Mock private RedisMarginCallEventSubscriber marginCallSubscriber;
    @Mock private RedisLiquidationEventSubscriber liquidationSubscriber;
    @Mock private RedisTrailingStopResponseEventSubscriber trailingStopSubscriber;
    @Mock private RedisTrailingStopCancelResponseEventSubscriber trailingStopCancelSubscriber;
    @Mock private RedisOtocoResponseEventSubscriber otocoSubscriber;
    @Mock private RedisOtocoCancelResponseEventSubscriber otocoCancelSubscriber;
    @Mock private RedisAlertResponseEventSubscriber alertSubscriber;
    @Mock private RedisAlertCancelResponseEventSubscriber alertCancelSubscriber;
    @Mock private RedisAlertFiredEventSubscriber alertFiredSubscriber;
    @Mock private RedisQueuePositionEventSubscriber queuePositionSubscriber;

    @InjectMocks
    private UserRedisSubscriptionRegistry registry;

    private static final int CHANNEL_COUNT = UserEventType.values().length;

    /** 사용자 한 명에 대해 기대하는 (구독자 → 채널) 연결 */
    private Map<MessageListener, ChannelTopic> expectedSubscriptions(String username) {
        Map<MessageListener, ChannelTopic> expected = new LinkedHashMap<>();
        expected.put(orderSubscriber, topic(UserEventType.ORDER, username));
        expected.put(tradeSubscriber, topic(UserEventType.TRADE, username));
        expected.put(cancelSubscriber, topic(UserEventType.CANCEL, username));
        expected.put(accountSubscriber, topic(UserEventType.ACCOUNT, username));
        expected.put(autoOrderSubscriber, topic(UserEventType.AUTO_ORDER, username));
        expected.put(autoCancelSubscriber, topic(UserEventType.AUTO_CANCEL, username));
        expected.put(marginCallSubscriber, topic(UserEventType.MARGIN_CALL, username));
        expected.put(liquidationSubscriber, topic(UserEventType.LIQUIDATION, username));
        expected.put(trailingStopSubscriber, topic(UserEventType.TRAILING_STOP, username));
        expected.put(trailingStopCancelSubscriber, topic(UserEventType.TRAILING_STOP_CANCEL, username));
        expected.put(otocoSubscriber, topic(UserEventType.OTOCO, username));
        expected.put(otocoCancelSubscriber, topic(UserEventType.OTOCO_CANCEL, username));
        expected.put(alertSubscriber, topic(UserEventType.ALERT, username));
        expected.put(alertCancelSubscriber, topic(UserEventType.ALERT_CANCEL, username));
        expected.put(alertFiredSubscriber, topic(UserEventType.ALERT_FIRED, username));
        expected.put(queuePositionSubscriber, topic(UserEventType.QUEUE_POSITION, username));
        return expected;
    }

    private static ChannelTopic topic(UserEventType type, String username) {
        return new ChannelTopic(type.channel(username));
    }

    private void verifyAllSubscribed(String username) {
        expectedSubscriptions(username).forEach((subscriber, topic) ->
                verify(container).addMessageListener(subscriber, topic));
    }

    private void verifyAllUnsubscribed(String username) {
        expectedSubscriptions(username).forEach((subscriber, topic) ->
                verify(container).removeMessageListener(subscriber, topic));
    }

    // ===================== 구독 =====================

    @Nested
    @DisplayName("subscribe")
    class Subscribe {

        @Test
        @DisplayName("사용자의 첫 세션이면 모든 사용자 이벤트 채널을 각 구독자에 연결한다")
        void firstSession_subscribesAllChannels() {
            registry.subscribe("session-A", "user1");

            verifyAllSubscribed("user1");
            verify(container, times(CHANNEL_COUNT)).addMessageListener(any(MessageListener.class), any(Topic.class));
            assertThat(registry.getAllConnectedUsernames()).containsExactly("user1");
        }

        @Test
        @DisplayName("같은 사용자의 두 번째 세션(다른 탭)은 채널을 다시 구독하지 않는다")
        void secondSession_doesNotResubscribe() {
            registry.subscribe("session-A", "user1");
            clearInvocations(container);

            registry.subscribe("session-B", "user1");

            verifyNoInteractions(container);
            assertThat(registry.getAllConnectedUsernames()).containsExactly("user1");
        }

        @Test
        @DisplayName("이미 등록된 세션이 다시 구독을 요청하면 무시한다 (참조 카운트 중복 증가 방지)")
        void sameSessionTwice_ignored() {
            registry.subscribe("session-A", "user1");
            registry.subscribe("session-A", "user1");
            clearInvocations(container);

            registry.disconnect("session-A");

            verifyAllUnsubscribed("user1");
            assertThat(registry.getAllConnectedUsernames()).isEmpty();
        }

        @Test
        @DisplayName("사용자마다 각자의 채널을 따로 구독한다")
        void differentUsers_subscribeSeparately() {
            registry.subscribe("session-A", "user1");
            registry.subscribe("session-B", "user2");

            verifyAllSubscribed("user1");
            verifyAllSubscribed("user2");
            assertThat(registry.getAllConnectedUsernames()).containsExactlyInAnyOrder("user1", "user2");
        }
    }

    // ===================== 해제 =====================

    @Nested
    @DisplayName("disconnect")
    class Disconnect {

        @Test
        @DisplayName("사용자의 세션이 남아 있으면 구독을 유지한다 (남은 탭이 알림을 계속 받음)")
        void otherSessionRemains_keepsSubscription() {
            registry.subscribe("session-A", "user1");
            registry.subscribe("session-B", "user1");

            registry.disconnect("session-A");

            verify(container, never()).removeMessageListener(any(MessageListener.class), any(Topic.class));
            assertThat(registry.getAllConnectedUsernames()).containsExactly("user1");
        }

        @Test
        @DisplayName("사용자의 마지막 세션이 끊기면 모든 채널 구독을 해제한다")
        void lastSession_unsubscribesAllChannels() {
            registry.subscribe("session-A", "user1");
            registry.subscribe("session-B", "user1");

            registry.disconnect("session-A");
            registry.disconnect("session-B");

            verifyAllUnsubscribed("user1");
            verify(container, times(CHANNEL_COUNT))
                    .removeMessageListener(any(MessageListener.class), any(Topic.class));
            assertThat(registry.getAllConnectedUsernames()).isEmpty();
        }

        @Test
        @DisplayName("한 사용자의 해제는 다른 사용자의 구독에 영향을 주지 않는다")
        void otherUserUnaffected() {
            registry.subscribe("session-A", "user1");
            registry.subscribe("session-B", "user2");

            registry.disconnect("session-A");

            verifyAllUnsubscribed("user1");
            expectedSubscriptions("user2").forEach((subscriber, topic) ->
                    verify(container, never()).removeMessageListener(subscriber, topic));
            assertThat(registry.getAllConnectedUsernames()).containsExactly("user2");
        }

        @Test
        @DisplayName("등록되지 않은 세션(익명 연결 등)이 끊기면 아무것도 하지 않는다")
        void unknownSession_noop() {
            registry.disconnect("anonymous-session");

            verifyNoInteractions(container);
        }

        @Test
        @DisplayName("같은 세션을 두 번 해제해도 두 번째는 무시한다")
        void disconnectTwice_ignored() {
            registry.subscribe("session-A", "user1");
            registry.subscribe("session-B", "user1");

            registry.disconnect("session-A");
            registry.disconnect("session-A");

            verify(container, never()).removeMessageListener(any(MessageListener.class), any(Topic.class));
            assertThat(registry.getAllConnectedUsernames()).containsExactly("user1");
        }

        @Test
        @DisplayName("모두 해제한 뒤 다시 접속하면 채널을 새로 구독한다")
        void resubscribeAfterFullDisconnect() {
            registry.subscribe("session-A", "user1");
            registry.disconnect("session-A");
            clearInvocations(container);

            registry.subscribe("session-C", "user1");

            verifyAllSubscribed("user1");
        }
    }

    @Test
    @DisplayName("접속자 목록은 복사본이라 반환값을 수정해도 내부 상태가 바뀌지 않는다")
    void connectedUsernames_isCopy() {
        registry.subscribe("session-A", "user1");

        Set<String> usernames = registry.getAllConnectedUsernames();
        usernames.clear();

        assertThat(registry.getAllConnectedUsernames()).containsExactly("user1");
    }
}
