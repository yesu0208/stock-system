package arile.toy.stocksystem.bffserver.otococancel.event.subscriber;

import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelErrorCode;
import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelResponseEvent;
import arile.toy.stocksystem.bffserver.otococancel.service.OtocoCancelResponsePushService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisOtocoCancelResponseEventSubscriberTest {

    private final OtocoCancelResponsePushService pushService = mock(OtocoCancelResponsePushService.class);
    private final RedisOtocoCancelResponseEventSubscriber subscriber = new RedisOtocoCancelResponseEventSubscriber(
            new ObjectMapper().findAndRegisterModules()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
            pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:otoco:cancel.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("OTOCO 취소 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다")
    void onMessage() {
        subscriber.onMessage(message("""
                {"otocoId": 1, "username": "user1", "stockCode": "005930", "entryDirection": "BELOW",
                 "entryTriggerPrice": 68000, "orderQuantity": 10, "success": false,
                 "errorCode": "ALREADY_COMPLETED"}
                """), null);

        ArgumentCaptor<OtocoCancelResponseEvent> captor = ArgumentCaptor.forClass(OtocoCancelResponseEvent.class);
        verify(pushService).push(captor.capture());

        OtocoCancelResponseEvent event = captor.getValue();
        assertThat(event.otocoId()).isEqualTo(1L);
        assertThat(event.username()).isEqualTo("user1");
        assertThat(event.stockCode()).isEqualTo("005930");
        assertThat(event.entryDirection()).isEqualTo(OtocoEntryDirection.BELOW);
        assertThat(event.entryTriggerPrice()).isEqualTo(68_000);
        assertThat(event.orderQuantity()).isEqualTo(10);
        assertThat(event.success()).isFalse();
        assertThat(event.errorCode()).isEqualTo(OtocoCancelErrorCode.ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
