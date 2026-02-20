package arile.toy.stocksystem.bffserver.external.stock.event.listener;

import arile.toy.stocksystem.bffserver.external.stock.event.manager.StockRealtimeRedisSubscriptionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockWebSocketSubscriptionEventListenerTest {

    @Mock
    private StockRealtimeRedisSubscriptionManager subscriptionManager;

    @InjectMocks
    private StockWebSocketSubscriptionEventListener listener;

    private final String sessionId = "sess1";

    @Test
    @DisplayName("Subscribe 이벤트 처리 시 subscribe 호출")
    void givenSubscribeEvent_whenHandleSubscribe_thenCallsSubscribe() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId(sessionId);
        String stockCode = "005930";
        accessor.setDestination("/sub/stock/" + stockCode);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

        // when
        listener.handleSubscribe(event);

        // then
        verify(subscriptionManager).subscribe(sessionId, stockCode);
    }

    @Test
    @DisplayName("Unsubscribe 이벤트 처리 시 unsubscribeAll 호출")
    void givenUnsubscribeEvent_whenHandleUnsubscribe_thenCallsUnsubscribeAll() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId(sessionId);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message);

        // when
        listener.handleUnsubscribe(event);

        // then
        verify(subscriptionManager).unsubscribeAll(sessionId);
    }

    @Test
    @DisplayName("Disconnect 이벤트 처리 시 disconnect 호출")
    void givenDisconnectEvent_whenHandleDisconnect_thenCallsDisconnect() {
        // given
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(sessionId);

        // when
        listener.handleDisconnect(event);

        // then
        verify(subscriptionManager).disconnect(sessionId);
    }
}
