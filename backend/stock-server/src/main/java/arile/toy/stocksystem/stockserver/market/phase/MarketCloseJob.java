package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.sevice.AutoOrderService;
import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.useraccount.service.UserAccountService;
import arile.toy.stocksystem.stockserver.userstock.service.UserStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketCloseJob {

    private final MarketCloseLock marketCloseLock;
    private final OrderService orderService;
    private final CancelService cancelService;
    private final AutoOrderService autoOrderService;
    private final AutoCancelService autoCancelService;
    private final UserAccountService userAccountService;
    private final UserStockService userStockService;
    private final MarketClosePublisher marketClosePublisher;
    private final ExternalStockProperties externalStockProperties;

    @Scheduled(cron = "0 40 15 * * MON-FRI", zone = "Asia/Seoul")
    public void runMarketCloseJob() {

        if (!marketCloseLock.acquire()) {
            log.info("[MarketCloseJob] Another instance already running market close job.");
            return;
        }

        log.info("[MarketCloseJob] market close job started.");

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

            Set<String> usernames = Stream.concat(
                            unfilledOrders.stream().map(OrderEntity::getUsername),
                            untriggeredAutoOrders.stream().map(AutoOrderEntity::getUsername)
                    )
                    .collect(Collectors.toSet());

            userAccountService.settleAccounts(usernames);
            userStockService.settleStocks(usernames);

            marketClosePublisher.publishMarketClose();

            log.info("[MarketCloseJob] market closing job finished.");

        } catch (Exception e) {
            log.error("[MarketCloseJob] market closing job failed.", e);

        } finally {
            marketCloseLock.release();
        }
    }
}