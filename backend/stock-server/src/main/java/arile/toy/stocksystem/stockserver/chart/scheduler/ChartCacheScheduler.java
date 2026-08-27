package arile.toy.stocksystem.stockserver.chart.scheduler;

import arile.toy.stocksystem.stockserver.chart.service.ChartCacheService;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChartCacheScheduler {

    private final ExternalStockProperties stockProperties;
    private final ChartCacheService chartCacheService;

    @PostConstruct
    public void initialFill() {
        stockProperties.getOpen().forEach(chartCacheService::refreshDailyChart);
        stockProperties.getOpen().forEach(chartCacheService::refreshMinuteChart);
    }

    @Scheduled(cron = "30 * 9-14 * * MON-FRI", zone = "Asia/Seoul")
    public void refreshMinuteChartsMorningToAfternoon() {
        refreshMinuteCharts();
    }

    @Scheduled(cron = "30 0-30 15 * * MON-FRI", zone = "Asia/Seoul")
    public void refreshMinuteChartsClosingWindow() {
        refreshMinuteCharts();
    }

    private void refreshMinuteCharts() {
        stockProperties.getOpen().forEach(chartCacheService::refreshMinuteChart);
    }

    @Scheduled(fixedRate = 600_000)
    public void refreshDailyCharts() {
        stockProperties.getOpen().forEach(chartCacheService::refreshDailyChart);
    }
}
