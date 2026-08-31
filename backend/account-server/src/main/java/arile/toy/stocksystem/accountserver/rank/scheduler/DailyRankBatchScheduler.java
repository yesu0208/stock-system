package arile.toy.stocksystem.accountserver.rank.scheduler;

import arile.toy.stocksystem.accountserver.rank.service.DailyRankBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyRankBatchScheduler {

    private static final String LOCK_KEY = "lock:rank:daily-batch";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final DailyRankBatchService dailyRankBatchService;

    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "Asia/Seoul")
    public void run() {

        if (!acquireLock()) {
            log.info("[DailyRankBatch] Another instance already running.");
            return;
        }

        try {
            dailyRankBatchService.runDailyBatch();
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }

    private boolean acquireLock() {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "LOCKED", LOCK_TTL));
    }
}
