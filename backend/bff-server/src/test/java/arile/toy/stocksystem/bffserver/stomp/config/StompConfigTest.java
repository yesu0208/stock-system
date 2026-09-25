package arile.toy.stocksystem.bffserver.stomp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StompConfigTest {

    private final StompConfig config = new StompConfig();

    @Test
    @DisplayName("주식·주문 웹소켓 접속 주소를 SockJS로 등록한다 (프론트 RealtimeContext의 접속 주소와 일치해야 함)")
    void registerStompEndpoints() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpoint = mock(StompWebSocketEndpointRegistration.class);
        given(registry.addEndpoint(anyString())).willReturn(endpoint);
        given(endpoint.setAllowedOriginPatterns(anyString())).willReturn(endpoint);
        given(endpoint.withSockJS()).willReturn(mock(SockJsServiceRegistration.class));

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws-stock");
        verify(registry).addEndpoint("/ws-order");
        verify(endpoint, times(2)).setAllowedOriginPatterns("*");
        verify(endpoint, times(2)).withSockJS();
    }

    @Test
    @DisplayName("구독은 /sub, 서버로 보내는 메시지는 /app, 사용자 전용 채널은 /user 접두사를 쓴다")
    void configureMessageBroker() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        given(registry.enableSimpleBroker("/sub")).willReturn(mock(SimpleBrokerRegistration.class));

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/sub");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
    }
}
