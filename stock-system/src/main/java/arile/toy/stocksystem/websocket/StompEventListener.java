package arile.toy.stocksystem.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompEventListener {

    private final WebSocketClient webSocketClient;

    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> codeRefCount = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        String destination = accessor.getDestination();
        if (destination == null) return;

        String code = extractCode(destination);

        sessionSubscriptions
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(code);

        int count = codeRefCount
                .computeIfAbsent(code, k -> new AtomicInteger(0))
                .incrementAndGet();

        if (count == 1) {
            log.info("REAL subscribe to external API server: {}", code);
            webSocketClient.subscribe(code);
        }

        log.info("session={} subscribe {}", sessionId, code);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Set<String> codes = sessionSubscriptions.get(sessionId);
        if (codes == null) return;

        for (String code : codes) {
            decrementAndUnsubscribeIfNeeded(code);
            log.info("session={} unsubscribe {}", sessionId, code);
        }

        sessionSubscriptions.remove(sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        Set<String> codes = sessionSubscriptions.remove(sessionId);
        if (codes == null) return;

        for (String code : codes) {
            decrementAndUnsubscribeIfNeeded(code);
            log.info("session={} disconnect unsubscribe {}", sessionId, code);
        }
    }

    private void decrementAndUnsubscribeIfNeeded(String code) {
        AtomicInteger counter = codeRefCount.get(code);
        if (counter == null) return;

        if (counter.decrementAndGet() == 0) {
            log.info("REAL unsubscribe to external API server: {}", code);
            webSocketClient.unsubscribe(code);
            codeRefCount.remove(code);
        }
    }

    private String extractCode(String destination) {
        return destination.substring(destination.lastIndexOf("/") + 1);
    }
}
