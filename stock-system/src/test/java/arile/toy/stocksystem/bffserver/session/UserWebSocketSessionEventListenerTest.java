package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class UserWebSocketSessionEventListenerTest {

    @Mock
    private UserRedisSubscriptionRegistry subscriptionRegistry;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private InitialDataService initialDataService;

    @InjectMocks
    private UserWebSocketSessionEventListener listener;

    private StompHeaderAccessor accessor;
    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("handleConnect 정상 처리")
    void handleConnect_subscribesUser() {
        accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(() -> "user1");
        accessor.setSessionId("sess1");

        SessionConnectEvent event = new SessionConnectEvent(this,
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null);

        listener.handleConnect(event);

        verify(subscriptionRegistry).subscribe("sess1", "user1");
    }

    @Test
    @DisplayName("handleConnect username 없으면 무시")
    void handleConnect_noUsername() {
        accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("sess2");
        SessionConnectEvent event = new SessionConnectEvent(this,
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null);

        listener.handleConnect(event);

        verifyNoInteractions(subscriptionRegistry);
    }

    @Test
    @DisplayName("handleDisconnect 호출")
    void handleDisconnect_callsRegistryDisconnect() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn("sess1");

        listener.handleDisconnect(event);

        verify(subscriptionRegistry).disconnect("sess1");
    }

    @Test
    @DisplayName("handleSubscribe account data 전송")
    void handleSubscribe_account() {
        accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> "user1");
        accessor.setDestination("/user/sub/account");

        SessionSubscribeEvent event = new SessionSubscribeEvent(this,
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

        AccountResponse mockResponse = mock(AccountResponse.class);
        Optional<AccountResponse> accountData = Optional.of(mockResponse);

        when(initialDataService.getAccountData("user1")).thenReturn(accountData);

        listener.handleSubscribe(event);

        verify(messagingTemplate).convertAndSendToUser(eq("user1"), eq("/sub/account"), eq(accountData.get()));
    }

    @Test
    @DisplayName("handleSubscribe order data 전송")
    void handleSubscribe_order() {
        accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> "user1");
        accessor.setDestination("/user/sub/order");

        SessionSubscribeEvent event = new SessionSubscribeEvent(this,
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

        OrderResponseMessage mockOrder = mock(OrderResponseMessage.class);
        Optional<List<OrderResponseMessage>> orderData = Optional.of(List.of(mockOrder));

        when(initialDataService.getOrderData("user1")).thenReturn(orderData);

        listener.handleSubscribe(event);

        verify(messagingTemplate).convertAndSendToUser(eq("user1"), eq("/sub/order"), eq(orderData.get()));
    }

    @Test
    @DisplayName("handleSubscribe stock data 전송")
    void handleSubscribe_stock() {
        String stockCode = "005930";
        accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> "user1");
        accessor.setDestination("/sub/stock/" + stockCode);

        SessionSubscribeEvent event = new SessionSubscribeEvent(this,
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

        BffServerBidAskPriceTickMessage mockBidAsk = mock(BffServerBidAskPriceTickMessage.class);
        BffServerTradePriceTickMessage mockTrade = mock(BffServerTradePriceTickMessage.class);

        Optional<BffServerBidAskPriceTickMessage> bidAskData = Optional.of(mockBidAsk);
        Optional<BffServerTradePriceTickMessage> tradeData = Optional.of(mockTrade);

        when(initialDataService.getBidAskPriceData(stockCode)).thenReturn(bidAskData);
        when(initialDataService.getTradePriceData(stockCode)).thenReturn(tradeData);

        listener.handleSubscribe(event);

        verify(messagingTemplate).convertAndSend(eq("/sub/stock/" + stockCode), eq(bidAskData.get()));
        verify(messagingTemplate).convertAndSend(eq("/sub/stock/" + stockCode), any(BffServerTradePriceClientTickMessage.class));
    }
}
