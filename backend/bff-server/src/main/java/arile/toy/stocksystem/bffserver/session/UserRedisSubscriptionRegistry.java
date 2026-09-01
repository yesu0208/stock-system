package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.event.subscriber.RedisAccountUpdateEventSubscriber;
import arile.toy.stocksystem.bffserver.autocancel.event.subscriber.RedisAutoCancelResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.autoorder.event.subscriber.RedisAutoOrderResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.cancel.event.subscriber.RedisCancelResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.leverage.event.subscriber.RedisLiquidationEventSubscriber;
import arile.toy.stocksystem.bffserver.leverage.event.subscriber.RedisMarginCallEventSubscriber;
import arile.toy.stocksystem.bffserver.order.event.subscriber.RedisOrderResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.trade.event.subscriber.RedisTradeResponseEventSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRedisSubscriptionRegistry {

    private final RedisMessageListenerContainer container;

    private final RedisOrderResponseEventSubscriber orderSubscriber;
    private final RedisAutoOrderResponseEventSubscriber autoOrderSubscriber;
    private final RedisTradeResponseEventSubscriber tradeSubscriber;
    private final RedisCancelResponseEventSubscriber cancelSubscriber;
    private final RedisAutoCancelResponseEventSubscriber autoCancelSubscriber;
    private final RedisAccountUpdateEventSubscriber accountSubscriber;
    private final RedisMarginCallEventSubscriber marginCallSubscriber;
    private final RedisLiquidationEventSubscriber liquidationSubscriber;

    private final ConcurrentHashMap<String, AtomicInteger> userRefCount = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Map<UserEventType, RedisSubscription>> subscriptions =
            new ConcurrentHashMap<>();

    public synchronized void subscribe(String sessionId, String username) {

        if (sessionUserMap.containsKey(sessionId)) {
            return;
        }

        sessionUserMap.put(sessionId, username);

        userRefCount.compute(username, (user, count) -> {
            if (count == null) {

                Map<UserEventType, RedisSubscription> userSubs =
                        createSubscriptions(user);

                subscriptions.put(user, userSubs);

                userSubs.values().forEach(sub ->
                        container.addMessageListener(sub.subscriber(), sub.topic())
                );

                log.info("Redis subscribe all user events username={}", user);
                return new AtomicInteger(1);
            }

            count.incrementAndGet();
            return count;
        });
    }

    public synchronized void disconnect(String sessionId) {

        String username = sessionUserMap.remove(sessionId);
        if (username == null) return;

        userRefCount.computeIfPresent(username, (user, count) -> {
            if (count.decrementAndGet() == 0) {

                Map<UserEventType, RedisSubscription> userSubs =
                        subscriptions.remove(user);

                if (userSubs != null) {
                    userSubs.values().forEach(sub ->
                            container.removeMessageListener(
                                    sub.subscriber(),
                                    sub.topic()
                            )
                    );
                }

                log.info("Redis unsubscribe all user events username={}", user);
                return null;
            }
            return count;
        });
    }

    public Set<String> getAllConnectedUsernames() {
        return new HashSet<>(userRefCount.keySet());
    }

    private Map<UserEventType, RedisSubscription> createSubscriptions(String username) {

        Map<UserEventType, RedisSubscription> map =
                new EnumMap<>(UserEventType.class);

        map.put(
                UserEventType.ORDER,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.ORDER.channel(username)),
                        orderSubscriber
                )
        );

        map.put(
                UserEventType.TRADE,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.TRADE.channel(username)),
                        tradeSubscriber
                )
        );

        map.put(
                UserEventType.CANCEL,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.CANCEL.channel(username)),
                        cancelSubscriber
                )
        );

        map.put(
                UserEventType.ACCOUNT,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.ACCOUNT.channel(username)),
                        accountSubscriber
                )
        );

        map.put(
                UserEventType.AUTO_ORDER,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.AUTO_ORDER.channel(username)),
                        autoOrderSubscriber
                )
        );

        map.put(
                UserEventType.AUTO_CANCEL,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.AUTO_CANCEL.channel(username)),
                        autoCancelSubscriber
                )
        );

        map.put(UserEventType.MARGIN_CALL,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.MARGIN_CALL.channel(username)),
                        marginCallSubscriber
                )
        );

        map.put(UserEventType.LIQUIDATION,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.LIQUIDATION.channel(username)),
                        liquidationSubscriber
                )
        );

        return map;
    }
}
