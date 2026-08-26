package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.event.StockDetailRedisPublisher;
import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDetailCrawlScheduler {

    private static final String LOCK_PREFIX = "stock:detail:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(4); // fixedRate(5s)보다 짧게

    private final StockDetailWatchRegistry watchRegistry;
    private final NaverStockCrawlerClient naverStockCrawlerClient;
    private final StockDetailRedisPublisher publisher;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedRate = 5_000)
    public void crawlActiveStocks() {
        Set<String> activeCodes = watchRegistry.getActiveCodes();
        if (activeCodes.isEmpty()) return;

        for (String stockCode : activeCodes) {
            if (!tryAcquireLock(stockCode)) {
                continue; // 다른 인스턴스가 이번 주기 담당
            }

            try {
                StockDetailTickMessage message = naverStockCrawlerClient.getStockDetailSummary(stockCode);
                publisher.publish(message);
            } catch (Exception e) {
                log.error("종목 상세 크롤링 실패. stockCode={}", stockCode, e);
            }
        }
    }

    private boolean tryAcquireLock(String stockCode) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + stockCode, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }
}