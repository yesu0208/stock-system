package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoService;
import arile.toy.stocksystem.stockserver.otococancel.service.OtocoCancelService;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopService;
import arile.toy.stocksystem.stockserver.trailingstopcancel.service.TrailingStopCancelService;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애프터마켓(16:00:05~20:00:00) 종료 후 미체결/미발동 주문을 정리하는 작업.
 * MarketCloseJob(정규장 15:40)과 완전히 동일한 구조(락/그룹 코디네이터/settleAll/
 * publishMarketClose)를 사용한다. "MARKET_CLOSED" 메시지는 실제로는
 * "정규장 마감 시각(15:30)"이 아니라 "미체결 정리 작업이 모든 그룹에서
 * 끝났다"는 완료 신호이므로, 애프터 정리 완료 시에도 동일하게 재사용.
 * (수익률·등급 배치(DailyRankBatchScheduler 등)는 정규장 마감 기준으로만
 * 하루 한 번 실행되며, 이 잡과는 무관하다.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AfterMarketCloseJob {

    private final AfterMarketCloseLock afterMarketCloseLock;
    private final OrderService orderService;
    private final CancelService cancelService;
    private final AutoOrderService autoOrderService;
    private final AutoCancelService autoCancelService;
    private final TrailingStopService trailingStopService;
    private final TrailingStopCancelService trailingStopCancelService;
    private final OtocoService otocoService;
    private final OtocoCancelService otocoCancelService;
    private final AccountApiClient accountApiClient;
    private final MarketClosePublisher marketClosePublisher;
    private final ExternalStockProperties externalStockProperties;
    private final AfterMarketCloseCoordinator afterMarketCloseCoordinator;

    @Scheduled(cron = "0 5 20 * * MON-FRI", zone = "Asia/Seoul")
    public void runAfterMarketCloseJob() {

        if (!afterMarketCloseLock.acquire()) {
            log.info("[AfterMarketCloseJob] Another instance already running after-market close job.");
            return;
        }

        log.info("[AfterMarketCloseJob] after-market close job started.");

        try {
            List<String> myStockCodes = externalStockProperties.getOpen();

            List<OrderEntity> unfilledOrders = orderService.findAllUnfilledOrders(myStockCodes);
            for (OrderEntity order : unfilledOrders) {
                cancelService.forceCancel(order.getOrderId());
            }

            List<AutoOrderEntity> untriggeredAutoOrders = autoOrderService.findAllUntriggeredAutoOrders(myStockCodes);
            for (AutoOrderEntity autoOrder : untriggeredAutoOrders) {
                autoCancelService.forceAutoCancel(autoOrder.getAutoOrderId());
            }

            log.info("[AfterMarketCloseJob] cancel finished for this group. stockCodes={}", myStockCodes);

            List<TrailingStopEntity> untriggeredTrailingStops = trailingStopService.findAllUntriggeredTrailingStops(myStockCodes);
            for (TrailingStopEntity trailingStop : untriggeredTrailingStops) {
                trailingStopCancelService.forceCancelTrailingStop(trailingStop.getTrailingStopId());
            }

            log.info("[AfterMarketCloseJob] trailing-stop cancel finished for this group. stockCodes={}", myStockCodes);

            List<OtocoEntity> unfinishedOtocos = otocoService.findAllUnfinishedOtocos(myStockCodes);
            for (OtocoEntity otoco : unfinishedOtocos) {
                otocoCancelService.forceCancelOtoco(otoco.getOtocoId());
            }

            log.info("[AfterMarketCloseJob] otoco cancel finished for this group. stockCodes={}", myStockCodes);

            boolean isLast = afterMarketCloseCoordinator.markDoneAndCheckLast();

            if (isLast) {
                accountApiClient.settleAll();
                marketClosePublisher.publishMarketClose("애프터마켓 마감 정리"); // [수정]
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
