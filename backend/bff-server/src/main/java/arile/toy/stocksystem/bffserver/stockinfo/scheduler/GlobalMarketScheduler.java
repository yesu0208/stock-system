package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.event.GlobalMarketRedisPublisher;
import arile.toy.stocksystem.bffserver.stockinfo.repository.GlobalMarketSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalMarketScheduler {

    private static final String LOCK_KEY = "market:global:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(8); // fixedRate(10s)보다 짧게

    private final NaverStockCrawlerClient naverStockCrawlerClient;
    private final StringRedisTemplate redisTemplate;
    private final GlobalMarketSnapshotRepository snapshotRepository;
    private final GlobalMarketRedisPublisher publisher;

    @Scheduled(fixedRate = 10_000)
    public void broadcastGlobalMarket() {
        if (!tryAcquireLock()) {
            return; // 다른 인스턴스가 이번 주기 담당
        }

        try {
            GlobalMarketResponse response = new GlobalMarketResponse(
                    naverStockCrawlerClient.getExchangeRates()
            );
            snapshotRepository.save(response);
            publisher.publish(response);
        } catch (Exception e) {
            log.error("환율/세계지수 브로드캐스트 실패", e);
        }
    }

    private boolean tryAcquireLock() {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }
}
