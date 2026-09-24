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

    /**
     * 종목 상세를 크롤링해 발행.
     * 스레드 풀(submit)에서 실행되면 밖으로 나간 예외는 로그 없이 사라지므로,
     * 락 획득(Redis 장애 등)을 포함한 모든 실패를 여기서 잡아 기록.
     */
    public void crawlAndPublish(String stockCode) {
        try {
            if (!tryAcquireLock(stockCode)) {
                return; // 다른 인스턴스가 이미 처리 중이거나 방금 처리함
            }

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
