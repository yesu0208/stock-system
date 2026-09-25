package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.external.stock.checker.MarketTimeChecker;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 애프터마켓(16:00:05~20:00:00) 종료 후 미체결/미발동 주문을 정리하는 작업.
 * MarketCloseJob(정규장 15:40)과 완전히 동일한 구조(락/그룹 코디네이터/settleAll/
 * publishMarketClose)를 사용한다. "MARKET_CLOSED" 메시지는 실제로는
 * "정규장 마감 시각(15:30)"이 아니라 "미체결 정리 작업이 모든 그룹에서
 * 끝났다"는 완료 신호이므로, 애프터 정리 완료 시에도 동일하게 재사용.
 *
 * 하루 정산 순서: 이 잡(20:05, 미체결 정리) → DailyLeverageBatchScheduler
 * (20:10, 이자 청구) → DailyRankBatchScheduler(20:15, 수익률·등급 배치).
 * 애프터마켓까지 반영된 최종 상태를 기준으로 이자·수익률·등급을 계산하기 위해
 * 정규장 마감 기준에서 애프터마켓 마감 기준으로 변경.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AfterMarketCloseJob {

    private final AfterMarketCloseLock afterMarketCloseLock;
    private final MarketCloseCleanupService marketCloseCleanupService;
    private final AccountApiClient accountApiClient;
    private final MarketClosePublisher marketClosePublisher;
    private final ExternalStockProperties externalStockProperties;
    private final AfterMarketCloseCoordinator afterMarketCloseCoordinator;
    private final MarketTimeChecker marketTimeChecker;

    @Scheduled(cron = "0 5 20 * * MON-FRI", zone = "Asia/Seoul")
    public void runAfterMarketCloseJob() {

        if (marketTimeChecker.isTodayHoliday()) {
            log.info("[AfterMarketCloseJob] Today is a registered holiday. Skip.");
            return;
        }

        if (!afterMarketCloseLock.acquire()) {
            log.info("[AfterMarketCloseJob] Another instance already running after-market close job.");
            return;
        }

        log.info("[AfterMarketCloseJob] after-market close job started.");

        try {
            // 정리 실패 건이 있어도 완료 표시·정산까지 진행 (정산이 묶인 예약금을 풀어 주는 안전망)
            marketCloseCleanupService.cleanUp(externalStockProperties.getOpen());

            boolean isLast = afterMarketCloseCoordinator.markDoneAndCheckLast();

            if (isLast) {
                accountApiClient.settleAll();
                marketClosePublisher.publishMarketClose("AFTER");
                log.info("[AfterMarketCloseJob] all groups finished cancel. settle-all triggered by this server.");
            } else {
                log.info("[AfterMarketCloseJob] waiting for other groups to finish cancel before settle.");
            }

            log.info("[AfterMarketCloseJob] after-market closing job finished.");

        } catch (Exception e) {
            log.error("[AfterMarketCloseJob] after-market closing job failed.", e);

        } finally {
            afterMarketCloseLock.release();
        }
    }
}
