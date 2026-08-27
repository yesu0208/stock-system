package arile.toy.stocksystem.bffserver.external.stock.event.manager;

import arile.toy.stocksystem.bffserver.chart.event.subscriber.RedisDailyCandleEventSubscriber;
import arile.toy.stocksystem.bffserver.chart.event.subscriber.RedisMinuteCandleEventSubscriber;
import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisBidAskPriceEventSubscriber;
import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisTradePriceEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.event.RedisStockDetailEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockDetailCrawlService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final RedisDailyCandleEventSubscriber dailyCandleSubscriber;
    private final RedisMinuteCandleEventSubscriber minuteCandleSubscriber;
    private final StockDetailWatchRegistry watchRegistry;
    private final StockDetailCrawlService crawlService;
    private final ExecutorService stockDetailCrawlExecutor;

    private final ConcurrentHashMap<String, AtomicInteger> stockRefCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    public void subscribe(String sessionId, String stockCode) {

        sessionSubscriptions
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(stockCode);

        AtomicBoolean isFirstSubscriber = new AtomicBoolean(false);

        stockRefCount.compute(stockCode, (code, count) -> {
            if (count == null) {
                container.addMessageListener(bidAskSubscriber, bidAskTopic(code));
                container.addMessageListener(tradeSubscriber, tradeTopic(code));
                container.addMessageListener(detailSubscriber, detailTopic(code));
                container.addMessageListener(dailyCandleSubscriber, dailyCandleTopic(code));
                container.addMessageListener(minuteCandleSubscriber, minuteCandleTopic(code));

                isFirstSubscriber.set(true);
                log.info("Redis subscribe stockCode={}", code);
                return new AtomicInteger(1);
            }
            count.incrementAndGet();
            return count;
        });

        watchRegistry.heartbeat(stockCode);

        if (isFirstSubscriber.get()) {
            stockDetailCrawlExecutor.submit(() -> crawlService.crawlAndPublish(stockCode));
        }
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
        for (String stockCode : stocks) decreaseRefCount(stockCode);
    }

    public void disconnect(String sessionId) {
        Set<String> stocks = sessionSubscriptions.remove(sessionId);
        if (stocks == null) return;
        for (String stockCode : stocks) decreaseRefCount(stockCode);
    }

    @Scheduled(fixedRate = 10_000)
    public void heartbeatActiveStocks() {
        stockRefCount.forEach((stockCode, count) -> {
            if (count.get() > 0) watchRegistry.heartbeat(stockCode);
        });
    }

    private void decreaseRefCount(String stockCode) {
        stockRefCount.computeIfPresent(stockCode, (code, count) -> {
            if (count.decrementAndGet() == 0) {
                container.removeMessageListener(bidAskSubscriber, bidAskTopic(code));
                container.removeMessageListener(tradeSubscriber, tradeTopic(code));
                container.removeMessageListener(detailSubscriber, detailTopic(code));
                container.removeMessageListener(dailyCandleSubscriber, dailyCandleTopic(code));
                container.removeMessageListener(minuteCandleSubscriber, minuteCandleTopic(code));

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

    private ChannelTopic dailyCandleTopic(String stockCode) {
        return new ChannelTopic("dailycandle." + stockCode + ":event");
    }

    private ChannelTopic minuteCandleTopic(String stockCode) {
        return new ChannelTopic("minutecandle." + stockCode + ":event");
    }
}
