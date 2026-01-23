package arile.toy.stocksystem.bffserver.stomp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@RequiredArgsConstructor
@EnableWebSocketMessageBroker
@Configuration
public class StompConnectionConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor interceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptor);
    }
}
