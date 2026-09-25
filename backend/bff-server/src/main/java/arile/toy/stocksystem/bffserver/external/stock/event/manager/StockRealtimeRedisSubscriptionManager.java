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

import java.util.List;
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

    /**
     * 구독 키(sessionId:subscriptionId) -> 종목코드.
     * 참조 카운트는 이 구독 단위로 증감. (구독 1건당 +1, 해제 1건당 -1)
     * 기존에는 증가는 구독 단위, 감소는 세션별 종목 Set 단위로 해서,
     * 같은 세션이 같은 종목을 두 번 구독하면 카운트가 0이 되지 않아 Redis 구독이 해제되지 않았음.
     */
    private final ConcurrentHashMap<String, String> subscriptionKeyToStockCode = new ConcurrentHashMap<>();

    public void subscribe(String sessionId, String subscriptionId, String stockCode) {

        // 같은 구독 ID가 중복 전달되면 한 번만 셈
        if (subscriptionKeyToStockCode.putIfAbsent(subscriptionKey(sessionId, subscriptionId), stockCode) != null) {
            return;
        }

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

    public void unsubscribeBySubscriptionId(String sessionId, String subscriptionId) {
        releaseSubscription(subscriptionKey(sessionId, subscriptionId));
    }

    /** 세션의 모든 구독을 하나씩 해제하며, 구독마다 참조 카운트를 1씩 낮춤 */
    public void disconnect(String sessionId) {
        String prefix = sessionId + ":";

        List<String> sessionKeys = subscriptionKeyToStockCode.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .toList();

        sessionKeys.forEach(this::releaseSubscription);
    }

    @Scheduled(fixedRate = 10_000)
    public void heartbeatActiveStocks() {
        stockRefCount.forEach((stockCode, count) -> {
            if (count.get() > 0) watchRegistry.heartbeat(stockCode);
        });
    }

    /**
     * 구독 하나를 해제하고, 실제로 지운 경우에만 참조 카운트를 낮춤.
     * 이미 없는 구독(모르는 ID, 또는 연결 종료와 구독 해제가 동시에 처리되어 먼저 지워진 경우)이면
     * 카운트를 다시 낮추지 않아, 다른 사용자가 보는 종목의 Redis 구독이 끊기지 않도록 함.
     */
    private void releaseSubscription(String subscriptionKey) {
        String stockCode = subscriptionKeyToStockCode.remove(subscriptionKey);
        if (stockCode == null) return;

        decreaseRefCount(stockCode);
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

    private String subscriptionKey(String sessionId, String subscriptionId) {
        return sessionId + ":" + subscriptionId;
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
