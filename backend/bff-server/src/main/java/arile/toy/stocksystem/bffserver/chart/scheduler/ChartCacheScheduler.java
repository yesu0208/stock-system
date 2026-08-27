package arile.toy.stocksystem.bffserver.chart.scheduler;

import arile.toy.stocksystem.bffserver.chart.service.ChartCacheService;
import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChartCacheScheduler {

    private final StockDetailWatchRegistry watchRegistry;
    private final ChartCacheService chartCacheService;

    @Scheduled(fixedRate = 60_000)
    public void refreshMinuteCharts() {
        watchRegistry.getActiveCodes().forEach(chartCacheService::refreshMinuteChart);
    }

    @Scheduled(fixedRate = 600_000)
    public void refreshDailyCharts() {
        watchRegistry.getActiveCodes().forEach(chartCacheService::refreshDailyChart);
    }
}
