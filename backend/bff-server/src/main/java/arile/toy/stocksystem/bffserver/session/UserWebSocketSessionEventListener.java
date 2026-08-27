package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.chart.dto.DailyChartSnapshotMessage;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteChartSnapshotMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import arile.toy.stocksystem.bffserver.stockinfo.repository.GlobalMarketSnapshotRepository;
import arile.toy.stocksystem.bffserver.stockinfo.repository.MarketMainSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserWebSocketSessionEventListener {

    private static final String MARKET_MAIN_DESTINATION = "/sub/market/main";
    private static final String GLOBAL_MARKET_DESTINATION = "/sub/market/global";

    private final UserRedisSubscriptionRegistry subscriptionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final InitialDataService initialDataService;
    private final MarketMainSnapshotRepository marketMainSnapshotRepository;
    private final GlobalMarketSnapshotRepository globalMarketSnapshotRepository;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = acc.getSessionId();
        String username = extractUsername(acc);

        if (username == null) {
            // 익명 연결: /sub/market/main 같은 공개 채널만 구독 가능
            log.debug("WS anonymous connect, sessionId={}", sessionId);
            return;
        }

        subscriptionRegistry.subscribe(sessionId, username);
        log.info("WS connect subscribe username={}, sessionId={}", username, sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        subscriptionRegistry.disconnect(sessionId);

        log.info("WS disconnect sessionId={}", sessionId);
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String destination = accessor.getDestination();
        if (destination == null) return;

        // 익명 사용자도 접근 가능한 공개 채널
        if (MARKET_MAIN_DESTINATION.equals(destination)) {
            sendMarketMainSnapshot(accessor);
            return;
        }

        if (GLOBAL_MARKET_DESTINATION.equals(destination)) {
            sendGlobalMarketSnapshot(accessor);
            return;
        }

        // 그 외 채널은 로그인 사용자 전용
        String username = extractUsername(accessor);
        if (username == null) return;

        if ("/user/sub/account".equals(destination)) {
            initialDataService.getAccountData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/account", data));
        } else if ("/user/sub/order".equals(destination)) {
            initialDataService.getOrderData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/order", data));
        } else if ("/user/sub/auto/order".equals(destination)) {
            initialDataService.getAutoOrderData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/auto/order", data));
        } else if (destination.startsWith("/sub/stock/")) {
            String stockCode = destination.substring("/sub/stock/".length());

            if ("summary".equals(stockCode)) {
                return;
            }

            initialDataService.getBidAskPriceData(stockCode)
                    .ifPresent(data -> messagingTemplate.convertAndSend(
                            "/sub/stock/" + stockCode, data));
            initialDataService.getTradePriceData(stockCode)
                    .ifPresent(data -> messagingTemplate.convertAndSend(
                            "/sub/stock/" + stockCode,
                            BffServerTradePriceClientTickMessage.fromTickMessage(data)));
            initialDataService.getStockDetailData(stockCode)
                    .ifPresent(data -> messagingTemplate.convertAndSend(
                            "/sub/stock/" + stockCode, data));

            log.info("[구독] 차트 데이터 조회 시작. stockCode={}", stockCode);
            initialDataService.getDailyChartData(stockCode)
                    .ifPresent(data -> {
                        log.info("[구독] 일봉 스냅샷 전송. stockCode={}, count={}", stockCode, data.size());
                        messagingTemplate.convertAndSend(
                                "/sub/stock/" + stockCode, DailyChartSnapshotMessage.of(stockCode, data));
                    });
            initialDataService.getMinuteChartData(stockCode)
                    .ifPresent(data -> {
                        log.info("[구독] 분봉 스냅샷 전송. stockCode={}, count={}", stockCode, data.size());
                        messagingTemplate.convertAndSend(
                                "/sub/stock/" + stockCode, MinuteChartSnapshotMessage.of(stockCode, data));
                    });
        }
    }

    private void sendMarketMainSnapshot(StompHeaderAccessor accessor) {
        MarketMainResponse snapshot = marketMainSnapshotRepository.getLatest();
        if (snapshot == null) return;

        messagingTemplate.convertAndSend(MARKET_MAIN_DESTINATION, snapshot);
    }

    private void sendGlobalMarketSnapshot(StompHeaderAccessor accessor) {
        GlobalMarketResponse snapshot = globalMarketSnapshotRepository.getLatest();
        if (snapshot == null) return;

        messagingTemplate.convertAndSend(GLOBAL_MARKET_DESTINATION, snapshot);
    }

    private String extractUsername(StompHeaderAccessor acc) {
        if (acc.getUser() != null) {
            return acc.getUser().getName();
        }
        return null;
    }
}