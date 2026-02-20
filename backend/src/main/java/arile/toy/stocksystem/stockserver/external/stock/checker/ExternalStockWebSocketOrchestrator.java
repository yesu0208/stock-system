package arile.toy.stocksystem.stockserver.external.stock.checker;

import arile.toy.stocksystem.stockserver.external.stock.approvalkey.ApprovalKeyService;
import arile.toy.stocksystem.stockserver.external.stock.listener.ExternalStockWebSocketClient;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.market.phase.MarketPhaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalStockWebSocketOrchestrator {

    private final ExternalStockWebSocketClient externalStockWebSocketClient;
    private final ApprovalKeyService approvalKeyService;
    private final ExternalStockProperties stockProperties;
    private final MarketTimeChecker marketTimeChecker;
    private final MarketPhaseService marketPhaseService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (marketTimeChecker.isMarketOpenNow()) {
            log.info("Server started during market hours");
            connectAndSubscribeIfNeeded();
            marketPhaseService.setScheduledMarkets();
        } else {
            log.info("Market closed at startup. Skip connect.");
            marketPhaseService.closeAllMarkets();
        }
    }

    @Scheduled(cron = "10 50 8 ? * MON-FRI", zone = "Asia/Seoul")
    public void connectAtMarketOpen() {
        log.info("Market open trigger");
        connectAndSubscribeIfNeeded();
        marketPhaseService.setScheduledMarkets();
    }

    @Scheduled(fixedDelay = 5_000)
    public void reconnectIfDisconnected() {
        if (!marketTimeChecker.isMarketOpenNow()) return;
        if (externalStockWebSocketClient.isConnected()) return;

        log.warn("WebSocket disconnected during market hours. Reconnecting...");
        connectAndSubscribeIfNeeded();
        marketPhaseService.setScheduledMarkets();
    }

    @Scheduled(cron = "50 39 15 ? * MON-FRI", zone = "Asia/Seoul")
    public void disconnectAtMarketClose() {
        externalStockWebSocketClient.disconnect();
        marketPhaseService.closeAllMarkets();
    }

    private synchronized void connectAndSubscribeIfNeeded() {
        if (externalStockWebSocketClient.isConnected()) return;

        String approvalKey = approvalKeyService.issueApprovalKey();
        externalStockWebSocketClient.connect(approvalKey);

        stockProperties.getOpen()
                .forEach(externalStockWebSocketClient::subscribe);
    }
}
