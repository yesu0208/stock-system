package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockDetailCrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDetailCrawlScheduler {

    private final StockDetailWatchRegistry watchRegistry;
    private final StockDetailCrawlService crawlService;

    @Scheduled(fixedRate = 5_000)
    public void crawlActiveStocks() {
        Set<String> activeCodes = watchRegistry.getActiveCodes();
        if (activeCodes.isEmpty()) return;

        for (String stockCode : activeCodes) {
            crawlService.crawlAndPublish(stockCode);
        }
    }
}