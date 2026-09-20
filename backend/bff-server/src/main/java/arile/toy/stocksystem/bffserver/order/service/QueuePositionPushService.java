package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.order.event.QueuePositionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueuePositionPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void push(QueuePositionEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.username(),
                "/sub/order/queue-position",
                event
        );
    }
}
