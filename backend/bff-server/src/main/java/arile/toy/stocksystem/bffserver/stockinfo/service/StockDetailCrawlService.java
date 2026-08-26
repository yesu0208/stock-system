package arile.toy.stocksystem.bffserver.stockinfo.service;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.event.StockDetailRedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockDetailCrawlService {

    private static final String LOCK_PREFIX = "stock:detail:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(4); // 스케줄러 주기(5s)보다 짧게

    private final NaverStockCrawlerClient naverStockCrawlerClient;
    private final StockDetailRedisPublisher publisher;
    private final StringRedisTemplate redisTemplate;

    public void crawlAndPublish(String stockCode) {

        if (!tryAcquireLock(stockCode)) {
            return; // 다른 인스턴스가 이미 처리 중이거나 방금 처리함
        }

        try {
            StockDetailTickMessage message = naverStockCrawlerClient.getStockDetailSummary(stockCode);
            publisher.publish(message);
        } catch (Exception e) {
            log.error("종목 상세 크롤링 실패. stockCode={}", stockCode, e);
        }
    }

    private boolean tryAcquireLock(String stockCode) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + stockCode, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }
}