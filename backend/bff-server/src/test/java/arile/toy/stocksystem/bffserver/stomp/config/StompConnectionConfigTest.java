package arile.toy.stocksystem.bffserver.stomp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StompConnectionConfigTest {

    @Test
    @DisplayName("STOMP 클라이언트 인바운드 채널에 JWT 인터셉터를 등록한다")
    void configureClientInboundChannel() {
        StompJwtChannelInterceptor interceptor = mock(StompJwtChannelInterceptor.class);
        ChannelRegistration registration = mock(ChannelRegistration.class);

        new StompConnectionConfig(interceptor).configureClientInboundChannel(registration);

        verify(registration).interceptors(interceptor);
    }
}
