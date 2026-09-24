package arile.toy.stocksystem.bffserver.trailingstop.event.subscriber;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResultCode;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopResponseEvent;
import arile.toy.stocksystem.bffserver.trailingstop.service.TrailingStopResponsePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisTrailingStopResponseEventSubscriberTest {

    private final TrailingStopResponsePushService pushService = mock(TrailingStopResponsePushService.class);
    private final RedisTrailingStopResponseEventSubscriber subscriber =
            new RedisTrailingStopResponseEventSubscriber(new ObjectMapper().findAndRegisterModules(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:trailing:stop.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("기준가 갱신 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다")
    void onMessage_trailingUpdated() {
        subscriber.onMessage(message("""
                {"trailingStopId": 1, "username": "user1", "stockCode": "005930", "trailingStopType": "SELL",
                 "leverageRatio": "SPOT", "orderQuantity": 10, "stopPercent": 3.0, "basePrice": 72000,
                 "triggerPrice": 69800, "orderTime": "2026-09-24T00:30:00Z",
                 "success": true, "resultCode": "TRAILING_UPDATED"}
                """), null);

        verify(pushService).push(new TrailingStopResponseEvent(1L, "user1", "005930", TrailingStopType.SELL,
                LeverageRatio.SPOT, 10, 3.0, 72_000, 69_800, Instant.parse("2026-09-24T00:30:00Z"),
                true, TrailingStopResultCode.TRAILING_UPDATED));
    }

    @Test
    @DisplayName("사용자명·결과 코드만 있는 발동 메시지도 나머지를 null로 채워 넘긴다")
    void onMessage_triggeredWithNulls() {
        subscriber.onMessage(message("""
                {"trailingStopId": null, "username": "user1", "stockCode": null, "trailingStopType": null,
                 "leverageRatio": null, "orderQuantity": null, "stopPercent": null, "basePrice": null,
                 "triggerPrice": null, "orderTime": null, "success": true, "resultCode": "TRIGGERED"}
                """), null);

        verify(pushService).push(new TrailingStopResponseEvent(null, "user1", null, null, null,
                null, null, null, null, null, true, TrailingStopResultCode.TRIGGERED));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
