package arile.toy.stocksystem.bffserver.chart.service;

import arile.toy.stocksystem.bffserver.chart.repository.ChartSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartCacheService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    private static final String DAILY_LOCK_PREFIX = "chart:daily:lock:";
    private static final String MINUTE_LOCK_PREFIX = "chart:minute:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(50);

    private final StockChartService stockChartService;
    private final StockMinuteChartService stockMinuteChartService;
    private final ChartSnapshotRepository chartSnapshotRepository;
    private final StringRedisTemplate redisTemplate;

    public void refreshDailyChart(String stockCode) {
        if (!tryAcquireLock(DAILY_LOCK_PREFIX + stockCode)) return;
        try {
            String to = LocalDate.now().format(DATE_FORMAT);
            String from = LocalDate.now().minusMonths(12).format(DATE_FORMAT);
            var candles = stockChartService.getDailyChart(stockCode, from, to);
            chartSnapshotRepository.saveDaily(stockCode, candles);
        } catch (Exception e) {
            log.error("일봉 캐시 갱신 실패. stockCode={}", stockCode, e);
        }
    }

    public void refreshMinuteChart(String stockCode) {
        if (!tryAcquireLock(MINUTE_LOCK_PREFIX + stockCode)) return;
        try {
            String date = LocalDate.now().format(DATE_FORMAT);
            String hour = LocalTime.now().format(TIME_FORMAT);
            var candles = stockMinuteChartService.getMinuteChart(stockCode, date, hour, 500);
            chartSnapshotRepository.saveMinute(stockCode, candles);
        } catch (Exception e) {
            log.error("분봉 캐시 갱신 실패. stockCode={}", stockCode, e);
        }
    }

    private boolean tryAcquireLock(String key) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL));
    }
}
