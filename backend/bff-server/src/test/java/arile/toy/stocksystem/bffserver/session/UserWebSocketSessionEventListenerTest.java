package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.chart.dto.DailyChartSnapshotMessage;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteChartSnapshotMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.bffserver.portfolio.dto.PortfolioResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.repository.GlobalMarketSnapshotRepository;
import arile.toy.stocksystem.bffserver.stockinfo.repository.MarketMainSnapshotRepository;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkSessionRegistry;
import arile.toy.stocksystem.bffserver.stocktalk.service.StockTalkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserWebSocketSessionEventListenerTest {

    private static final Principal USER1 = () -> "user1";
    private static final String SESSION_ID = "session-A";
    private static final String SNAPSHOT_SUBSCRIBE = "/user/sub/stock/005930/snapshot";
    private static final String SNAPSHOT_DESTINATION = "/sub/stock/005930/snapshot";

    private UserRedisSubscriptionRegistry subscriptionRegistry;
    private SimpMessagingTemplate messagingTemplate;
    private InitialDataService initialDataService;
    private MarketMainSnapshotRepository marketMainSnapshotRepository;
    private GlobalMarketSnapshotRepository globalMarketSnapshotRepository;
    private StockTalkSessionRegistry stockTalkSessionRegistry;
    private StockTalkService stockTalkService;
    private UserWebSocketSessionEventListener listener;

    @BeforeEach
    void setUp() {
        subscriptionRegistry = mock(UserRedisSubscriptionRegistry.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        initialDataService = mock(InitialDataService.class);
        marketMainSnapshotRepository = mock(MarketMainSnapshotRepository.class);
        globalMarketSnapshotRepository = mock(GlobalMarketSnapshotRepository.class);
        stockTalkSessionRegistry = new StockTalkSessionRegistry();
        stockTalkService = mock(StockTalkService.class);

        listener = new UserWebSocketSessionEventListener(subscriptionRegistry, messagingTemplate, initialDataService,
                marketMainSnapshotRepository, globalMarketSnapshotRepository, stockTalkSessionRegistry, stockTalkService);
    }

    private static Message<byte[]> stompMessage(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(SESSION_ID);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private void subscribe(String destination, Principal user) {
        listener.handleSubscribe(new SessionSubscribeEvent(this,
                stompMessage(StompCommand.SUBSCRIBE, destination, user), user));
    }

    // ===================== 연결 · 해제 =====================

    @Nested
    @DisplayName("연결 · 해제")
    class ConnectAndDisconnect {

        @Test
        @DisplayName("로그인 사용자가 연결하면 세션 ID로 사용자 이벤트 채널을 구독한다")
        void connect_loggedIn() {
            listener.handleConnect(new SessionConnectEvent(this,
                    stompMessage(StompCommand.CONNECT, null, USER1), USER1));

            verify(subscriptionRegistry).subscribe(SESSION_ID, "user1");
        }

        @Test
        @DisplayName("익명 연결은 사용자 이벤트 채널을 구독하지 않는다")
        void connect_anonymous() {
            listener.handleConnect(new SessionConnectEvent(this,
                    stompMessage(StompCommand.CONNECT, null, null), null));

            verifyNoInteractions(subscriptionRegistry);
        }

        @Test
        @DisplayName("연결이 끊기면 사용자 구독을 해제하고, 참여 중이던 종목톡에서 모두 자동 퇴장시킨다")
        void disconnect_leavesStockTalks() {
            stockTalkSessionRegistry.registerJoin(SESSION_ID, "user1", "005930");
            stockTalkSessionRegistry.registerJoin(SESSION_ID, "user1", "000660");

            listener.handleDisconnect(new SessionDisconnectEvent(this,
                    stompMessage(StompCommand.DISCONNECT, null, USER1), SESSION_ID, CloseStatus.NORMAL));

            verify(subscriptionRegistry).disconnect(SESSION_ID);
            verify(stockTalkService).leave("005930", "user1");
            verify(stockTalkService).leave("000660", "user1");
            assertThat(stockTalkSessionRegistry.removeSession(SESSION_ID)).isNull();
        }

        @Test
        @DisplayName("종목톡에 참여하지 않은 세션이 끊기면 퇴장 처리를 하지 않는다")
        void disconnect_withoutStockTalk() {
            listener.handleDisconnect(new SessionDisconnectEvent(this,
                    stompMessage(StompCommand.DISCONNECT, null, null), SESSION_ID, CloseStatus.NORMAL));

            verify(subscriptionRegistry).disconnect(SESSION_ID);
            verifyNoInteractions(stockTalkService);
        }
    }

    // ===================== 시장 공개 채널 =====================

    @Nested
    @DisplayName("시장 공개 채널 구독")
    class PublicChannels {

        @Test
        @DisplayName("국내 시장 메인: 최신 스냅샷이 있으면 채널로 보낸다")
        void marketMain() {
            MarketMainResponse snapshot = mock(MarketMainResponse.class);
            given(marketMainSnapshotRepository.getLatest()).willReturn(snapshot);

            subscribe("/sub/market/main", null);

            verify(messagingTemplate).convertAndSend("/sub/market/main", snapshot);
        }

        @Test
        @DisplayName("해외 시장: 최신 스냅샷이 있으면 채널로 보낸다")
        void globalMarket() {
            GlobalMarketResponse snapshot = mock(GlobalMarketResponse.class);
            given(globalMarketSnapshotRepository.getLatest()).willReturn(snapshot);

            subscribe("/sub/market/global", null);

            verify(messagingTemplate).convertAndSend("/sub/market/global", snapshot);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/sub/market/main", "/sub/market/global"})
        @DisplayName("시장 스냅샷이 아직 없으면 보내지 않는다")
        void marketSnapshotMissing(String destination) {
            subscribe(destination, null);

            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("구독 주소가 없는 메시지는 무시한다")
        void nullDestination() {
            subscribe(null, USER1);

            verifyNoInteractions(messagingTemplate, initialDataService);
        }
    }

    // ===================== 종목 스냅샷 =====================

    @Nested
    @DisplayName("종목 스냅샷 채널 구독")
    class StockSnapshot {

        private final BffServerBidAskPriceTickMessage bidAsk = mock(BffServerBidAskPriceTickMessage.class);
        private final StockDetailTickMessage detail = mock(StockDetailTickMessage.class);

        @BeforeEach
        void givenSnapshots() {
            doReturn(Optional.of(bidAsk)).when(initialDataService).getBidAskPriceData("005930");
            doReturn(Optional.of(new BffServerTradePriceTickMessage(TickMessageType.TRADEPRICE, "005930", "090001",
                    71_000, 1_000, 70_000, "1.43", 70_000, 71_500, 69_800, 10, 1_000, 71_000_000L,
                    400, 600, "1", 900))).when(initialDataService).getTradePriceData("005930");
            doReturn(Optional.of(detail)).when(initialDataService).getStockDetailData("005930");
            doReturn(Optional.of(List.of())).when(initialDataService).getDailyChartData("005930");
            doReturn(Optional.of(List.of())).when(initialDataService).getMinuteChartData("005930");
        }

        /** 다섯 가지 스냅샷이 모두 세션 ID 헤더를 달고 이 세션의 스냅샷 주소로만 전송되었는지 확인 */
        @SuppressWarnings("unchecked")
        private void verifyAllSnapshotsSentToSession() {
            ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
            ArgumentCaptor<Map<String, Object>> headers = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate, times(5)).convertAndSendToUser(
                    eq(SESSION_ID), eq(SNAPSHOT_DESTINATION), payloads.capture(), headers.capture());

            assertThat(headers.getAllValues())
                    .allSatisfy(h -> assertThat(SimpMessageHeaderAccessor.getSessionId(h)).isEqualTo(SESSION_ID));

            assertThat(payloads.getAllValues()).contains(bidAsk, detail);
            assertThat(payloads.getAllValues()).hasAtLeastOneElementOfType(DailyChartSnapshotMessage.class);
            assertThat(payloads.getAllValues()).hasAtLeastOneElementOfType(MinuteChartSnapshotMessage.class);
            assertThat(payloads.getAllValues())
                    .filteredOn(BffServerTradePriceClientTickMessage.class::isInstance)
                    .singleElement()
                    .satisfies(p -> {
                        BffServerTradePriceClientTickMessage trade = (BffServerTradePriceClientTickMessage) p;
                        assertThat(trade.curPrice()).isEqualTo(71_000);
                        assertThat(trade.prevClosePrice()).isEqualTo(70_000);
                    });

            // 공용 종목 채널로는 아무것도 보내지 않는다 (다른 구독자에게 재전송 방지)
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("로그인 사용자: 호가·체결가·상세·일봉·분봉 스냅샷을 이 세션에게만 보낸다")
        void loggedIn_sendsToSessionOnly() {
            subscribe(SNAPSHOT_SUBSCRIBE, USER1);

            verifyAllSnapshotsSentToSession();
        }

        @Test
        @DisplayName("로그인하지 않은 사용자도 세션 기준으로 같은 스냅샷을 받는다 (로그인 전 화면의 시세·차트 표시)")
        void anonymous_sendsToSessionOnly() {
            subscribe(SNAPSHOT_SUBSCRIBE, null);

            verifyAllSnapshotsSentToSession();
        }

        @Test
        @DisplayName("공용 종목 채널(/sub/stock/{code}) 구독만으로는 스냅샷을 보내지 않는다")
        void publicStockChannel_noSnapshot() {
            subscribe("/sub/stock/005930", USER1);

            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("스냅샷이 없는 데이터는 보내지 않는다")
        void missingSnapshots_skipped() {
            subscribe("/user/sub/stock/000660/snapshot", null);

            verify(messagingTemplate, never())
                    .convertAndSendToUser(anyString(), anyString(), any(Object.class), anyMap());
        }

        @Test
        @DisplayName("종목코드가 비어 있으면 아무것도 조회하지 않는다")
        void blankStockCode_ignored() {
            subscribe("/user/sub/stock//snapshot", USER1);

            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("종목코드가 빈 스냅샷 구독(/user/sub/stock//snapshot)이면 아무것도 조회·전송하지 않는다")
        void stockSnapshot_blankStockCode_ignored() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setSessionId("session-A");
            accessor.setDestination("/user/sub/stock//snapshot");

            listener.handleSubscribe(new SessionSubscribeEvent(this,
                    MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders())));

            verifyNoInteractions(initialDataService, messagingTemplate);
        }

        @Test
        @DisplayName("세션 정보가 없는 스냅샷 구독이면 보낼 대상이 없으므로 아무것도 조회·전송하지 않는다")
        void stockSnapshot_noSession_ignored() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setDestination("/user/sub/stock/005930/snapshot");

            listener.handleSubscribe(new SessionSubscribeEvent(this,
                    MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders())));

            verifyNoInteractions(initialDataService, messagingTemplate);
        }

        @Test
        @DisplayName("/snapshot으로 끝나지 않는 사용자 종목 주소는 스냅샷 채널로 보지 않고 아무것도 보내지 않는다")
        void userStockWithoutSnapshotSuffix_ignored() {
            subscribe("/user/sub/stock/005930", USER1);

            verifyNoInteractions(messagingTemplate);
        }
    }

    // ===================== 사용자 전용 채널 =====================

    @Nested
    @DisplayName("사용자 전용 채널 구독")
    class UserChannels {

        @Test
        @DisplayName("/user/sub/account: 본인에게 계좌 초기 데이터를 보낸다")
        void account() {
            AccountResponse account = mock(AccountResponse.class);
            given(initialDataService.getAccountData("user1")).willReturn(Optional.of(account));

            subscribe("/user/sub/account", USER1);

            verify(messagingTemplate).convertAndSendToUser("user1", "/sub/account", account);
        }

        @Test
        @DisplayName("/user/sub/portfolio: 본인에게 포트폴리오 초기 데이터를 보낸다")
        void portfolio() {
            PortfolioResponse portfolio = new PortfolioResponse("user1", 1_000_000L, 1_000_000L, 100.0, List.of());
            given(initialDataService.getPortfolioData("user1")).willReturn(Optional.of(portfolio));

            subscribe("/user/sub/portfolio", USER1);

            verify(messagingTemplate).convertAndSendToUser("user1", "/sub/portfolio", portfolio);
        }

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "/user/sub/order,         /sub/order",
                "/user/sub/auto/order,    /sub/auto/order",
                "/user/sub/trailing-stop, /sub/trailing-stop",
                "/user/sub/otoco,         /sub/otoco",
                "/user/sub/alert,         /sub/alert"
        })
        @DisplayName("미체결·예약 주문 목록 채널: 본인에게 초기 목록을 보낸다")
        void pendingLists(String subscribeDestination, String userDestination) {
            doReturn(Optional.of(List.of())).when(initialDataService).getOrderData("user1");
            doReturn(Optional.of(List.of())).when(initialDataService).getAutoOrderData("user1");
            doReturn(Optional.of(List.of())).when(initialDataService).getTrailingStopData("user1");
            doReturn(Optional.of(List.of())).when(initialDataService).getOtocoData("user1");
            doReturn(Optional.of(List.of())).when(initialDataService).getAlertData("user1");

            subscribe(subscribeDestination, USER1);

            verify(messagingTemplate).convertAndSendToUser("user1", userDestination, List.of());
        }

        @Test
        @DisplayName("초기 데이터가 없으면 보내지 않는다 (예: 계좌 미생성)")
        void noData_skipped() {
            given(initialDataService.getAccountData("user1")).willReturn(Optional.empty());

            subscribe("/user/sub/account", USER1);

            verifyNoInteractions(messagingTemplate);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/user/sub/account", "/user/sub/order", "/user/sub/alert"})
        @DisplayName("로그인하지 않은 사용자는 사용자 전용 채널 초기 데이터를 받지 않는다")
        void anonymous_skipped(String destination) {
            subscribe(destination, null);

            verifyNoInteractions(initialDataService, messagingTemplate);
        }

        @Test
        @DisplayName("처리 대상이 아닌 채널은 무시한다")
        void unknownDestination_ignored() {
            subscribe("/user/sub/unknown", USER1);

            verifyNoInteractions(initialDataService, messagingTemplate);
        }
    }
}
