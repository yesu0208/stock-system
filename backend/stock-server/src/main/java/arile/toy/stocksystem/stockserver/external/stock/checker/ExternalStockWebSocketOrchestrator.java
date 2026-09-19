package arile.toy.stocksystem.stockserver.external.stock.checker;

import arile.toy.stocksystem.stockserver.external.stock.approvalkey.ApprovalKeyService;
import arile.toy.stocksystem.stockserver.external.stock.listener.ExternalStockWebSocketClient;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.market.phase.MarketPhaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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

    @Order(10)
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (marketTimeChecker.isMarketOpenNow()) {
            log.info("Server started during market hours");
            marketPhaseService.setScheduledMarkets();
            connectAndSubscribeIfNeeded();
        } else {
            log.info("Market closed at startup. Skip connect.");
            marketPhaseService.closeAllMarkets();
        }
    }

    @Scheduled(cron = "35 49 8 ? * MON-FRI", zone = "Asia/Seoul")
    public void preConnectBeforeMorningCall() {
        log.info("Pre-connect before morning call (30s buffer)");
        connectAndSubscribeIfNeeded();
    }

    @Scheduled(cron = "5 50 8 ? * MON-FRI", zone = "Asia/Seoul")
    public void connectAtMorningCall() {
        log.info("Morning call trigger");
        connectAndSubscribeIfNeeded();
        marketPhaseService.openMorningCall();
    }

    @Scheduled(cron = "0 0 9 ? * MON-FRI", zone = "Asia/Seoul")
    public void openRegularMarket() {
        marketPhaseService.openRegularMarket();
    }

    @Scheduled(cron = "0 20 15 ? * MON-FRI", zone = "Asia/Seoul")
    public void openClosingCall() {
        marketPhaseService.openClosingCall();
    }

    // [수정] 15:30:00 → 15:38:00 — closeMarketAfterClosingCall(tick 기반, 종목별 실제
    // 종가 확정 시점 감지)이 우선 동작하도록 시간을 벌어주고, 거래가 뜸해서 tick이
    // 안 들어온 종목만 이 고정 크론이 안전망으로 정리
    @Scheduled(cron = "0 38 15 ? * MON-FRI", zone = "Asia/Seoul")
    public void closeRegularMarket() {
        marketPhaseService.closeScheduledOpenMarkets();
    }

    @Scheduled(cron = "5 0 16 ? * MON-FRI", zone = "Asia/Seoul")
    public void openAfterMarket() {
        log.info("After-market trigger");
        connectAndSubscribeIfNeeded();
        marketPhaseService.openAfterMarket();
    }

    @Scheduled(fixedDelay = 5_000)
    public void reconnectIfDisconnected() {
        if (!marketTimeChecker.shouldMaintainConnection()) return;
        if (externalStockWebSocketClient.isConnected()) return;

        log.warn("WebSocket disconnected. Reconnecting...");
        connectAndSubscribeIfNeeded();
        marketPhaseService.setScheduledMarkets();
    }

    @Scheduled(cron = "0 0 20 ? * MON-FRI", zone = "Asia/Seoul")
    public void disconnectAtAfterMarketClose() {
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
