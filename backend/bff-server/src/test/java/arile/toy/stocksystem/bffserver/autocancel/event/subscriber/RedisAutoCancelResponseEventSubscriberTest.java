package arile.toy.stocksystem.bffserver.autocancel.event.subscriber;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.bffserver.autocancel.service.AutoCancelResponsePushService;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
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

class RedisAutoCancelResponseEventSubscriberTest {

    private final AutoCancelResponsePushService pushService = mock(AutoCancelResponsePushService.class);
    private final RedisAutoCancelResponseEventSubscriber subscriber =
            new RedisAutoCancelResponseEventSubscriber(new ObjectMapper(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:auto:cancel.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("자동 주문 취소 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다")
    void onMessage() {
        subscriber.onMessage(message("""
                {"autoOrderId": 1, "username": "user1", "stockCode": "005930", "autoOrderType": "SELL",
                 "triggerPrice": 72000, "orderPrice": 71500, "orderQuantity": 5,
                 "success": false, "errorCode": "ALREADY_TRIGGERED"}
                """), null);

        verify(pushService).push(AutoCancelResponseEvent.of(1L, "user1", "005930", AutoOrderType.SELL,
                72_000, 71_500, 5, false, AutoCancelErrorCode.ALREADY_TRIGGERED));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
