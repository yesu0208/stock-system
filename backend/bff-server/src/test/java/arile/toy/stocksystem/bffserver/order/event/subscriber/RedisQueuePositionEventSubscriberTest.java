package arile.toy.stocksystem.bffserver.order.event.subscriber;

import arile.toy.stocksystem.bffserver.order.event.QueuePositionEvent;
import arile.toy.stocksystem.bffserver.order.service.QueuePositionPushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisQueuePositionEventSubscriberTest {

    private final QueuePositionPushService pushService = mock(QueuePositionPushService.class);
    private final RedisQueuePositionEventSubscriber subscriber =
            new RedisQueuePositionEventSubscriber(new ObjectMapper(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:order:queue-position.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("대기열 순번 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다")
    void onMessage() {
        subscriber.onMessage(message("""
                {"orderId": 1, "username": "user1", "stockCode": "005930", "quantityAhead": 1250}
                """), null);

        verify(pushService).push(new QueuePositionEvent(1L, "user1", "005930", 1_250L));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
