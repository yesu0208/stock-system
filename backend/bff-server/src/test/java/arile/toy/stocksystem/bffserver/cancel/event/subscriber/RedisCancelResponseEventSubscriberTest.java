package arile.toy.stocksystem.bffserver.cancel.event.subscriber;

import arile.toy.stocksystem.bffserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.bffserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.bffserver.cancel.service.CancelResponsePushService;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
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

class RedisCancelResponseEventSubscriberTest {

    private final CancelResponsePushService pushService = mock(CancelResponsePushService.class);
    private final RedisCancelResponseEventSubscriber subscriber =
            new RedisCancelResponseEventSubscriber(new ObjectMapper().findAndRegisterModules(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:cancel.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("취소 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다")
    void onMessage() {
        subscriber.onMessage(message("""
                {"orderId": 1, "username": "user1", "stockCode": "005930", "orderType": "BUY",
                 "orderPrice": 70000, "orderQuantity": 10, "success": false, "errorCode": "ALREADY_FILLED"}
                """), null);

        verify(pushService).push(CancelResponseEvent.of(1L, "user1", "005930", OrderType.BUY,
                70_000, 10, false, CancelErrorCode.ALREADY_FILLED));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
