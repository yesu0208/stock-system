package arile.toy.stocksystem.bffserver.external.stock.event.manager;

import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisBidAskPriceEventSubscriber;
import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisTradePriceEventSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockRealtimeRedisSubscriptionManager {

    private final RedisMessageListenerContainer container;
    private final RedisBidAskPriceEventSubscriber bidAskSubscriber;
    private final RedisTradePriceEventSubscriber tradeSubscriber;

    private final ConcurrentHashMap<String, AtomicInteger> stockRefCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    public void subscribe(String sessionId, String stockCode) {

        sessionSubscriptions
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(stockCode);

        stockRefCount.compute(stockCode, (code, count) -> {
            if (count == null) {
                container.addMessageListener(bidAskSubscriber, bidAskTopic(code));
                container.addMessageListener(tradeSubscriber, tradeTopic(code));

                log.info("Redis subscribe stockCode={}", code);
                return new AtomicInteger(1);
            }
            count.incrementAndGet();
            return count;
        });
    }

    public void unsubscribe(String sessionId, String stockCode) {

        Set<String> stocks = sessionSubscriptions.get(sessionId);
        if (stocks != null && stocks.remove(stockCode)) {
            decreaseRefCount(stockCode);
        }
    }

    public void unsubscribeAll(String sessionId) {
        Set<String> stocks = sessionSubscriptions.remove(sessionId);
        if (stocks == null) return;

        for (String stockCode : stocks) {
            decreaseRefCount(stockCode);
        }
    }

    public void disconnect(String sessionId) {

        Set<String> stocks = sessionSubscriptions.remove(sessionId);
        if (stocks == null) return;

        for (String stockCode : stocks) {
            decreaseRefCount(stockCode);
        }
    }

    private void decreaseRefCount(String stockCode) {
        stockRefCount.computeIfPresent(stockCode, (code, count) -> {
            if (count.decrementAndGet() == 0) {
                container.removeMessageListener(bidAskSubscriber, bidAskTopic(code));
                container.removeMessageListener(tradeSubscriber, tradeTopic(code));

                log.info("Redis unsubscribe stockCode={}", code);
                return null;
            }
            return count;
        });
    }

    private ChannelTopic bidAskTopic(String stockCode) {
        return new ChannelTopic("bidask." + stockCode + ":event");
    }

    private ChannelTopic tradeTopic(String stockCode) {
        return new ChannelTopic("trade." + stockCode + ":event");
    }
}


