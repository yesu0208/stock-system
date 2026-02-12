package arile.toy.stocksystem.bffserver.external.stock.event.listener;

import arile.toy.stocksystem.bffserver.account.service.AccountPushService;
import arile.toy.stocksystem.bffserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.bffserver.session.UserRedisSubscriptionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockSummaryTickEventListener {

    private final AccountPushService accountPushService;
    private final UserRedisSubscriptionRegistry subscriptionManager;

    @EventListener
    public void handleAccountUpdate(StockSummaryTickEvent event) {
        try {
            for (String username : subscriptionManager.getAllConnectedUsernames()) {
                accountPushService.push(username);
            }

        } catch (Exception e) {
            log.error("Failed to push account updates", e);
        }
    }
}
