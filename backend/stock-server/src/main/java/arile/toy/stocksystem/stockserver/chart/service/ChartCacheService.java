package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.repository.ChartSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartCacheService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    private final StockChartService stockChartService;
    private final StockMinuteChartService stockMinuteChartService;
    private final ChartSnapshotRepository chartSnapshotRepository;

    public void refreshDailyChart(String stockCode) {
        long start = System.currentTimeMillis();
        log.info("일봉 캐시 갱신 시작. stockCode={}", stockCode);
        try {
            String to = LocalDate.now().format(DATE_FORMAT);
            String from = LocalDate.now().minusMonths(12).format(DATE_FORMAT);
            var candles = stockChartService.getDailyChart(stockCode, from, to);
            chartSnapshotRepository.saveDaily(stockCode, candles);
            log.info("일봉 캐시 갱신 완료. stockCode={}, count={}, elapsedMs={}",
                    stockCode, candles.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("일봉 캐시 갱신 실패. stockCode={}, elapsedMs={}",
                    stockCode, System.currentTimeMillis() - start, e);
        }
    }

    public void refreshMinuteChart(String stockCode) {
        long start = System.currentTimeMillis();
        log.info("분봉 캐시 갱신 시작. stockCode={}", stockCode);
        try {
            String date = LocalDate.now().format(DATE_FORMAT);
            String hour = LocalTime.now().format(TIME_FORMAT);
            var candles = stockMinuteChartService.getMinuteChart(stockCode, date, hour, 500);
            chartSnapshotRepository.saveMinute(stockCode, candles);
            log.info("분봉 캐시 갱신 완료. stockCode={}, count={}, elapsedMs={}",
                    stockCode, candles.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("분봉 캐시 갱신 실패. stockCode={}, elapsedMs={}",
                    stockCode, System.currentTimeMillis() - start, e);
        }
    }
}
