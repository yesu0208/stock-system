package arile.toy.stocksystem.bffserver.external.stock.event.listener;

import arile.toy.stocksystem.bffserver.external.stock.event.manager.StockRealtimeRedisSubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockWebSocketSubscriptionEventListener {

    private final StockRealtimeRedisSubscriptionManager subscriptionManager;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = acc.getSessionId();
        String destination = acc.getDestination();
        String stockCode = extractStockCode(destination);

        if (stockCode != null) {
            subscriptionManager.subscribe(sessionId, stockCode);
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = acc.getSessionId();

        subscriptionManager.unsubscribeAll(sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        subscriptionManager.disconnect(sessionId);
    }

    private String extractStockCode(String destination) {
        if (destination == null) return null;
        if (destination.equals("/sub/stock/summary")) return null;
        if (destination.startsWith("/sub/stock/")) {
            return destination.substring("/sub/stock/".length());
        }
        return null;
    }
}
