package arile.toy.stocksystem.bffserver.otoco.event.subscriber;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.bffserver.otoco.event.OtocoResponseEvent;
import arile.toy.stocksystem.bffserver.otoco.service.OtocoResponsePushService;
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

class RedisOtocoResponseEventSubscriberTest {

    private final OtocoResponsePushService pushService = mock(OtocoResponsePushService.class);
    private final RedisOtocoResponseEventSubscriber subscriber =
            new RedisOtocoResponseEventSubscriber(new ObjectMapper().findAndRegisterModules(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:otoco.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("OTOCO 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다 (남은 진입 수량 포함)")
    void onMessage() {
        subscriber.onMessage(message("""
                {"otocoId": 1, "username": "user1", "stockCode": "005930", "entryDirection": "ABOVE",
                 "leverageRatio": "SPOT", "orderQuantity": 10, "entryTriggerPrice": 70000,
                 "tpTriggerPrice": 75000, "slTriggerPrice": 65000, "otocoStatus": "ENTRY_ORDER_PLACED",
                 "orderTime": "2026-09-24T00:30:00Z", "success": true,
                 "resultCode": "ENTRY_PARTIALLY_FILLED", "entryRemainingQuantity": 4}
                """), null);

        verify(pushService).push(new OtocoResponseEvent(1L, "user1", "005930", OtocoEntryDirection.ABOVE,
                LeverageRatio.SPOT, 10, 70_000, 75_000, 65_000, OtocoStatus.ENTRY_ORDER_PLACED,
                Instant.parse("2026-09-24T00:30:00Z"), true, OtocoResultCode.ENTRY_PARTIALLY_FILLED, 4));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
