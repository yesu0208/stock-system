package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserWebSocketSessionEventListener {

    private final UserRedisSubscriptionRegistry subscriptionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final InitialDataService initialDataService;

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

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = extractUsername(accessor);
        if (username == null) return;

        String destination = accessor.getDestination();
        if (destination == null) return;

        if ("/user/sub/account".equals(destination)) {
            initialDataService.getAccountData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username,
                            "/sub/account",
                            data
                    ));
        } else if ("/user/sub/order".equals(destination)) {
            initialDataService.getOrderData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username,
                            "/sub/order",
                            data
                    ));
        } else if ("/user/sub/auto/order".equals(destination)) {
            initialDataService.getAutoOrderData(username)
                    .ifPresent(data -> messagingTemplate.convertAndSendToUser(
                            username,
                            "/sub/auto/order",
                            data
                    ));
        } else if (destination.startsWith("/sub/stock/")) {
            String stockCode = destination.substring("/sub/stock/".length());
            initialDataService.getBidAskPriceData(stockCode)
                    .ifPresent(data -> messagingTemplate.convertAndSend(
                            "/sub/stock/" + stockCode,
                            data
                    ));
            initialDataService.getTradePriceData(stockCode)
                    .ifPresent(data -> messagingTemplate.convertAndSend(
                            "/sub/stock/" + stockCode,
                            BffServerTradePriceClientTickMessage.fromTickMessage(data)
                    ));
        }
    }

    private String extractUsername(StompHeaderAccessor acc) {
        if (acc.getUser() != null) {
            return acc.getUser().getName();
        }
        return null;
    }
}
