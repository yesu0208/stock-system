package arile.toy.stocksystem.bffserver.trailingstopcancel.event.subscriber;

import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelErrorCode;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
import arile.toy.stocksystem.bffserver.trailingstopcancel.service.TrailingStopCancelResponsePushService;
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

class RedisTrailingStopCancelResponseEventSubscriberTest {

    private final TrailingStopCancelResponsePushService pushService = mock(TrailingStopCancelResponsePushService.class);
    private final RedisTrailingStopCancelResponseEventSubscriber subscriber =
            new RedisTrailingStopCancelResponseEventSubscriber(new ObjectMapper(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:trailing:stop:cancel.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("트레일링 스탑 취소 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다")
    void onMessage() {
        subscriber.onMessage(message("""
                {"trailingStopId": 1, "username": "user1", "stockCode": "005930", "trailingStopType": "BUY",
                 "triggerPrice": 72100, "orderQuantity": 5, "success": false, "errorCode": "ALREADY_TRIGGERED"}
                """), null);

        verify(pushService).push(new TrailingStopCancelResponseEvent(1L, "user1", "005930", TrailingStopType.BUY,
                72_100, 5, false, TrailingStopCancelErrorCode.ALREADY_TRIGGERED));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
