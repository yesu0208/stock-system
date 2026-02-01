package arile.toy.stocksystem.bffserver.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserWebSocketSessionEventListener {

    private final UserRedisSubscriptionRegistry subscriptionRegistry;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = acc.getSessionId();
        String username = extractUsername(acc);

        if (username == null) {
            log.warn("WS connect without username, sessionId={}", sessionId);
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

    private String extractUsername(StompHeaderAccessor acc) {
        if (acc.getUser() != null) {
            return acc.getUser().getName();
        }
        return null;
    }
}
