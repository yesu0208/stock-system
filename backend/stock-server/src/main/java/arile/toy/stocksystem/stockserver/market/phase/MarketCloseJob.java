package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.external.stock.checker.MarketTimeChecker;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketCloseJob {

    private final MarketCloseLock marketCloseLock;
    private final MarketCloseCleanupService marketCloseCleanupService;
    private final AccountApiClient accountApiClient;
    private final MarketClosePublisher marketClosePublisher;
    private final ExternalStockProperties externalStockProperties;
    private final MarketCloseCoordinator marketCloseCoordinator;
    private final MarketTimeChecker marketTimeChecker;

    @Scheduled(cron = "0 40 15 * * MON-FRI", zone = "Asia/Seoul")
    public void runMarketCloseJob() {

        if (marketTimeChecker.isTodayHoliday()) {
            log.info("[MarketCloseJob] Today is a registered holiday. Skip.");
            return;
        }

        if (!marketCloseLock.acquire()) {
            log.info("[MarketCloseJob] Another instance already running market close job.");
            return;
        }

        log.info("[MarketCloseJob] market close job started.");

        try {
            // 정리 실패 건이 있어도 완료 표시·정산까지 진행 (정산이 묶인 예약금을 풀어 주는 안전망)
            marketCloseCleanupService.cleanUp(externalStockProperties.getOpen());

            boolean isLast = marketCloseCoordinator.markDoneAndCheckLast();

            if (isLast) {
                accountApiClient.settleAll();
                marketClosePublisher.publishMarketClose("REGULAR");
                log.info("[MarketCloseJob] all groups finished cancel. settle-all triggered by this server.");
            } else {
                log.info("[MarketCloseJob] waiting for other groups to finish cancel before settle.");
            }

            log.info("[MarketCloseJob] market closing job finished.");

        } catch (Exception e) {
            log.error("[MarketCloseJob] market closing job failed.", e);

        } finally {
            marketCloseLock.release();
        }
    }
}
