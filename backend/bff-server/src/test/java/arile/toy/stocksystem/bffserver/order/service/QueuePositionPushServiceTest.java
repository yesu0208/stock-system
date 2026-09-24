package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.order.event.QueuePositionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueuePositionPushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private QueuePositionPushService service;

    @Test
    @DisplayName("대기열 순번 이벤트를 주문 소유자 전용 채널로 그대로 보낸다")
    void push() {
        QueuePositionEvent event = new QueuePositionEvent(1L, "user1", "005930", 1_250L);

        service.push(event);

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/order/queue-position", event);
    }
}
