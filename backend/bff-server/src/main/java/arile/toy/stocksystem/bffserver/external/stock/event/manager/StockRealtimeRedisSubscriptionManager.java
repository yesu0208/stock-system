package arile.toy.stocksystem.bffserver.external.stock.event.manager;

import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisBidAskPriceEventSubscriber;
import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisTradePriceEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.event.subscriber.RedisStockDetailEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Service
@RequiredArgsConstructor
@Slf4j
public class StockRealtimeRedisSubscriptionManager {

    private final RedisMessageListenerContainer container;
    private final RedisBidAskPriceEventSubscriber bidAskSubscriber;
    private final RedisTradePriceEventSubscriber tradeSubscriber;
    private final RedisStockDetailEventSubscriber detailSubscriber;
    private final StockDetailWatchRegistry watchRegistry;

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
                container.addMessageListener(detailSubscriber, detailTopic(code));

                log.info("Redis subscribe stockCode={}", code);
                return new AtomicInteger(1);
            }
            count.incrementAndGet();
            return count;
        });

        watchRegistry.heartbeat(stockCode);
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

    // 로컬에서 여전히 활성 상태인 종목들을 전역 레지스트리에 계속 살아있다고 알림
    @Scheduled(fixedRate = 10_000)
    public void heartbeatActiveStocks() {
        stockRefCount.forEach((stockCode, count) -> {
            if (count.get() > 0) {
                watchRegistry.heartbeat(stockCode);
            }
        });
    }

    private void decreaseRefCount(String stockCode) {
        stockRefCount.computeIfPresent(stockCode, (code, count) -> {
            if (count.decrementAndGet() == 0) {
                container.removeMessageListener(bidAskSubscriber, bidAskTopic(code));
                container.removeMessageListener(tradeSubscriber, tradeTopic(code));
                container.removeMessageListener(detailSubscriber, detailTopic(code));

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

    private ChannelTopic detailTopic(String stockCode) {
        return new ChannelTopic("stockdetail." + stockCode + ":event");
    }
}
