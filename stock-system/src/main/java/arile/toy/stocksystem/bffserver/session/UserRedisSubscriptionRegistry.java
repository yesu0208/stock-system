package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.event.subscriber.RedisAccountUpdateEventSubscriber;
import arile.toy.stocksystem.bffserver.order.event.subscriber.RedisOrderResponseEventSubscriber;
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
    private final RedisAccountUpdateEventSubscriber accountSubscriber;

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
                UserEventType.ACCOUNT,
                new RedisSubscription(
                        new ChannelTopic(UserEventType.ACCOUNT.channel(username)),
                        accountSubscriber
                )
        );
        return map;
    }
}

