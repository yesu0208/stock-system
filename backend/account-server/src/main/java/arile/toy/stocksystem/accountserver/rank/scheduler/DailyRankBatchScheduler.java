package arile.toy.stocksystem.accountserver.rank.scheduler;

import arile.toy.stocksystem.accountserver.market.holiday.HolidayRegistry;
import arile.toy.stocksystem.accountserver.rank.publisher.RankUpdatedPublisher;
import arile.toy.stocksystem.accountserver.rank.service.DailyRankBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyRankBatchScheduler {

    private static final String LOCK_KEY = "lock:rank:daily-batch";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final DailyRankBatchService dailyRankBatchService;
    private final RankUpdatedPublisher rankUpdatedPublisher;
    private final HolidayRegistry holidayRegistry;

    /**
     * 20:15 — DailyLeverageBatchScheduler(20:10 이자 청구) 직후 실행.
     * 애프터마켓까지 반영된 최종 상태를 기준으로 수익률·랭크를 계산하기 위해
     * 정규장 마감(15:50) → 애프터마켓 마감(20:15)으로 이동.
     */
    @Scheduled(cron = "0 15 20 * * MON-FRI", zone = "Asia/Seoul")
    public void run() {

        if (holidayRegistry.isHoliday(LocalDate.now())) {
            log.info("[DailyRankBatch] Today is a registered holiday. Skip.");
            return;
        }

        if (!acquireLock()) {
            log.info("[DailyRankBatch] Another instance already running.");
            return;
        }

        try {
            dailyRankBatchService.runDailyBatch();
            rankUpdatedPublisher.publish();
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }

    private boolean acquireLock() {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "LOCKED", LOCK_TTL));
    }
}
