package arile.toy.stocksystem.bffserver.autoorder.event.subscriber;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.service.AutoOrderResponsePushService;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
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

class RedisAutoOrderResponseEventSubscriberTest {

    private final AutoOrderResponsePushService pushService = mock(AutoOrderResponsePushService.class);
    private final RedisAutoOrderResponseEventSubscriber subscriber =
            new RedisAutoOrderResponseEventSubscriber(new ObjectMapper().findAndRegisterModules(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:auto:order.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("자동 주문 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다 (발동 결과 코드 포함)")
    void onMessage() {
        subscriber.onMessage(message("""
                {"autoOrderId": 1, "username": "user1", "stockCode": "005930", "autoOrderType": "BUY",
                 "leverageRatio": "SPOT", "triggerPrice": 68000, "orderPrice": 69000, "orderQuantity": 10,
                 "orderTime": "2026-09-24T00:30:00Z", "success": true, "resultCode": "TRIGGERED"}
                """), null);

        verify(pushService).push(new AutoOrderResponseEvent(1L, "user1", "005930", AutoOrderType.BUY,
                LeverageRatio.SPOT, 68_000, 69_000, 10, Instant.parse("2026-09-24T00:30:00Z"),
                true, AutoOrderResultCode.TRIGGERED));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
