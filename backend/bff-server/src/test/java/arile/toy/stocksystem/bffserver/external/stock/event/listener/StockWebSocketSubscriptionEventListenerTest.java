package arile.toy.stocksystem.bffserver.external.stock.event.listener;

import arile.toy.stocksystem.bffserver.external.stock.event.manager.StockRealtimeRedisSubscriptionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StockWebSocketSubscriptionEventListenerTest {

    private final StockRealtimeRedisSubscriptionManager subscriptionManager =
            mock(StockRealtimeRedisSubscriptionManager.class);
    private final StockWebSocketSubscriptionEventListener listener =
            new StockWebSocketSubscriptionEventListener(subscriptionManager);

    private static Message<byte[]> stompMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-A");
        accessor.setSubscriptionId("sub-1");
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private void subscribe(String destination) {
        listener.handleSubscribe(new SessionSubscribeEvent(this, stompMessage(StompCommand.SUBSCRIBE, destination)));
    }

    @Test
    @DisplayName("종목 채널을 구독하면 세션·구독 ID와 종목코드로 실시간 구독을 요청한다")
    void subscribe_stockChannel() {
        subscribe("/sub/stock/005930");

        verify(subscriptionManager).subscribe("session-A", "sub-1", "005930");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/sub/stock/summary",
            "/user/sub/stock/005930/snapshot",
            "/sub/stock-talk/005930",
            "/sub/market/main",
            "/user/sub/account"
    })
    @DisplayName("종목 채널이 아닌 구독은 실시간 구독을 요청하지 않는다 (전 종목 요약·스냅샷·종목톡·시장·사용자 채널)")
    void subscribe_nonStockChannel_ignored(String destination) {
        subscribe(destination);

        verifyNoInteractions(subscriptionManager);
    }

    @ParameterizedTest(name = "destination=[{0}]")
    @NullSource
    @DisplayName("구독 주소가 없으면 무시한다")
    void subscribe_noDestination_ignored(String destination) {
        subscribe(destination);

        verifyNoInteractions(subscriptionManager);
    }

    @Test
    @DisplayName("구독을 해제하면 세션·구독 ID로 해제를 요청한다 (종목 여부는 매니저가 판단)")
    void unsubscribe() {
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, stompMessage(StompCommand.UNSUBSCRIBE, null)));

        verify(subscriptionManager).unsubscribeBySubscriptionId("session-A", "sub-1");
    }

    @Test
    @DisplayName("연결이 끊기면 세션의 모든 실시간 구독 해제를 요청한다")
    void disconnect() {
        listener.handleDisconnect(new SessionDisconnectEvent(this,
                stompMessage(StompCommand.DISCONNECT, null), "session-A", CloseStatus.NORMAL));

        verify(subscriptionManager).disconnect("session-A");
    }
}
