package arile.toy.stocksystem.accountserver.leverage.scheduler;

import arile.toy.stocksystem.accountserver.leverage.publisher.InterestAppliedPublisher;
import arile.toy.stocksystem.accountserver.leverage.service.LeverageDailyBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyLeverageBatchScheduler {

    private static final String LOCK_KEY = "lock:leverage:daily-batch";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final LeverageDailyBatchService leverageDailyBatchService;
    private final InterestAppliedPublisher interestAppliedPublisher;

    /**
     * DailyRankBatchScheduler(15:50)보다 먼저 실행
     * RP 등급 계산(TotalAssetCalculator)이 레버리지 포지션의 대출금을 기준으로 순자산을 산정하므로,
     * 이자 누적이 반영된 이후에 등급 배치가 돌아야 정확
     *
     * 15:45 — MarketCloseJob(15:40 주문정리) 직후,
     * DailyRankBatchScheduler(15:50) 이전에 실행되도록 앞당김.
     */
    @Scheduled(cron = "0 45 15 * * MON-FRI", zone = "Asia/Seoul")
    public void run() {

        if (!acquireLock()) {
            log.info("[LeverageDailyBatch] Another instance already running.");
            return;
        }

        try {
            leverageDailyBatchService.runDailyLeverageBatch();
            interestAppliedPublisher.publish();
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }

    private boolean acquireLock() {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "LOCKED", LOCK_TTL));
    }
}
