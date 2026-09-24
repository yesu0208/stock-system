package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.chart.dto.DailyChartSnapshotMessage;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteChartSnapshotMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import arile.toy.stocksystem.bffserver.stockinfo.repository.GlobalMarketSnapshotRepository;
import arile.toy.stocksystem.bffserver.stockinfo.repository.MarketMainSnapshotRepository;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkSessionRegistry;
import arile.toy.stocksystem.bffserver.stocktalk.service.StockTalkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
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

    /** 종목 초기 스냅샷 전용 채널: 클라이언트는 /user/sub/stock/{code}/snapshot 을 구독 */
    private static final String STOCK_SNAPSHOT_SUBSCRIBE_PREFIX = "/user/sub/stock/";
    private static final String STOCK_DESTINATION_PREFIX = "/sub/stock/";
    private static final String STOCK_SNAPSHOT_SUFFIX = "/snapshot";

    private final UserRedisSubscriptionRegistry subscriptionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final InitialDataService initialDataService;
    private final MarketMainSnapshotRepository marketMainSnapshotRepository;
    private final GlobalMarketSnapshotRepository globalMarketSnapshotRepository;
    private final StockTalkSessionRegistry stockTalkSessionRegistry;
    private final StockTalkService stockTalkService;

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

        StockTalkSessionRegistry.SessionParticipation participation =
                stockTalkSessionRegistry.removeSession(sessionId);

        if (participation != null) {
            participation.tickers().forEach(ticker ->
                    stockTalkService.leave(ticker, participation.username()));
            log.info("[StockTalk] session disconnected, auto-leave username={}, tickers={}, sessionId={}",
                    participation.username(), participation.tickers(), sessionId);
        }

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

        // 종목 초기 스냅샷 채널: 구독한 세션에게만 전송 (로그인 전 화면의 시세·차트 표시를 위해 익명 포함)
        // 공용 종목 채널(/sub/stock/{code})로 보내면 이미 보고 있던 구독자 전원이 스냅샷을 다시 받게 되므로 분리함
        if (destination.startsWith(STOCK_SNAPSHOT_SUBSCRIBE_PREFIX) && destination.endsWith(STOCK_SNAPSHOT_SUFFIX)) {
            String stockCode = destination.substring(
                    STOCK_SNAPSHOT_SUBSCRIBE_PREFIX.length(),
                    destination.length() - STOCK_SNAPSHOT_SUFFIX.length());
            sendStockSnapshots(accessor.getSessionId(), stockCode);
            return;
        }

        // 그 외 채널은 로그인 사용자 전용
        String username = extractUsername(accessor);
        if (username == null) return;

        if ("/user/sub/account".equals(destination)) {
            initialDataService.getAccountData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/account", data));
        } else if ("/user/sub/portfolio".equals(destination)) {
            initialDataService.getPortfolioData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/portfolio", data));
        } else if ("/user/sub/order".equals(destination)) {
            initialDataService.getOrderData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/order", data));
        } else if ("/user/sub/auto/order".equals(destination)) {
            initialDataService.getAutoOrderData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/auto/order", data));
        } else if ("/user/sub/trailing-stop".equals(destination)) {
            initialDataService.getTrailingStopData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/trailing-stop", data));
        } else if ("/user/sub/otoco".equals(destination)) {
            initialDataService.getOtocoData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/otoco", data));
        } else if ("/user/sub/alert".equals(destination)) {
            initialDataService.getAlertData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username, "/sub/alert", data));
        }
    }

    /** 종목 스냅샷 채널을 구독한 세션에게만 호가·체결가·종목 상세·일봉·분봉 스냅샷을 전송 */
    private void sendStockSnapshots(String sessionId, String stockCode) {

        if (sessionId == null || stockCode.isBlank()) {
            return;
        }

        String snapshotDestination = STOCK_DESTINATION_PREFIX + stockCode + STOCK_SNAPSHOT_SUFFIX;

        initialDataService.getBidAskPriceData(stockCode)
                .ifPresent(data -> sendToSession(sessionId, snapshotDestination, data));
        initialDataService.getTradePriceData(stockCode)
                .ifPresent(data -> sendToSession(sessionId, snapshotDestination,
                        BffServerTradePriceClientTickMessage.fromTickMessage(data)));
        initialDataService.getStockDetailData(stockCode)
                .ifPresent(data -> sendToSession(sessionId, snapshotDestination, data));

        log.info("[구독] 차트 데이터 조회 시작. stockCode={}", stockCode);
        initialDataService.getDailyChartData(stockCode)
                .ifPresent(data -> {
                    log.info("[구독] 일봉 스냅샷 전송. stockCode={}, count={}", stockCode, data.size());
                    sendToSession(sessionId, snapshotDestination, DailyChartSnapshotMessage.of(stockCode, data));
                });
        initialDataService.getMinuteChartData(stockCode)
                .ifPresent(data -> {
                    log.info("[구독] 분봉 스냅샷 전송. stockCode={}, count={}", stockCode, data.size());
                    sendToSession(sessionId, snapshotDestination, MinuteChartSnapshotMessage.of(stockCode, data));
                });
    }

    /**
     * 특정 세션에게만 메시지를 보냄. 세션 ID 헤더가 있으면 Spring이 사용자 이름 대신 세션으로 대상을 찾으므로
     * 로그인하지 않은 익명 세션에도 전달됨. (클라이언트는 /user{destination}을 구독)
     */
    private void sendToSession(String sessionId, String destination, Object payload) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload, headers.getMessageHeaders());
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
