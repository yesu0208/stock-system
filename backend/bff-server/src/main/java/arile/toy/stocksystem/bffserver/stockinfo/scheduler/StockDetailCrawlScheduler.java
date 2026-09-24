package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockDetailCrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDetailCrawlScheduler {

    private final StockDetailWatchRegistry watchRegistry;
    private final StockDetailCrawlService crawlService;
    private final ExecutorService stockDetailCrawlExecutor;

    /**
     * 감시 중인 종목의 상세 정보를 크롤링
     * 네트워크 호출은 전용 스레드 풀에 맡겨, 공용 스케줄러 스레드가 크롤링 지연에 묶여
     * 다른 주기 작업(장 상태 재동기화, 종목 감시 갱신 등)이 멈추지 않도록 함.
     * 같은 종목의 중복 크롤링은 StockDetailCrawlService의 Redis 락이 막음.
     */
    @Scheduled(fixedRate = 5_000)
    public void crawlActiveStocks() {
        Set<String> activeCodes = watchRegistry.getActiveCodes();
        if (activeCodes == null || activeCodes.isEmpty()) return;

        for (String stockCode : activeCodes) {
            try {
                stockDetailCrawlExecutor.submit(() -> crawlService.crawlAndPublish(stockCode));
            } catch (RejectedExecutionException e) {
                // 서버 종료 중 스레드 풀이 닫힌 경우. 한 종목 실패로 나머지 등록이 멈추지 않도록 계속 진행
                log.warn("종목 상세 크롤링 작업 등록 실패. stockCode={}", stockCode);
            }
        }
    }
}
