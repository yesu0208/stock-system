package arile.toy.stocksystem.bffserver.alert.event.subscriber;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertErrorCode;
import arile.toy.stocksystem.bffserver.alert.event.AlertFiredEvent;
import arile.toy.stocksystem.bffserver.alert.event.AlertResponseEvent;
import arile.toy.stocksystem.bffserver.alert.service.AlertFiredPushService;
import arile.toy.stocksystem.bffserver.alert.service.AlertResponsePushService;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelErrorCode;
import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelResponseEvent;
import arile.toy.stocksystem.bffserver.alertcancel.event.subscriber.RedisAlertCancelResponseEventSubscriber;
import arile.toy.stocksystem.bffserver.alertcancel.service.AlertCancelResponsePushService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisAlertEventSubscribersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static Message message(String channel, String json) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("알림 등록 응답")
    class Response {

        private final AlertResponsePushService pushService = mock(AlertResponsePushService.class);
        private final RedisAlertResponseEventSubscriber subscriber =
                new RedisAlertResponseEventSubscriber(OBJECT_MAPPER, pushService);

        @Test
        @DisplayName("등록 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다 (등록 시각 포함)")
        void onMessage() {
            subscriber.onMessage(message("user:alert.user1:event", """
                    {"alertId": 1, "username": "user1", "stockCode": "005930", "direction": "ABOVE",
                     "triggerPrice": 70000, "registeredTime": "2026-09-24T00:30:00Z",
                     "success": true, "errorCode": null}
                    """), null);

            verify(pushService).push(new AlertResponseEvent(1L, "user1", "005930", AlertDirection.ABOVE,
                    70_000, Instant.parse("2026-09-24T00:30:00Z"), true, null));
        }

        @Test
        @DisplayName("실패 응답의 오류 코드도 파싱한다")
        void onMessage_failure() {
            subscriber.onMessage(message("user:alert.user1:event", """
                    {"alertId": null, "username": "user1", "stockCode": "005930", "direction": "BELOW",
                     "triggerPrice": 65000, "registeredTime": null, "success": false, "errorCode": "INTERNAL_ERROR"}
                    """), null);

            verify(pushService).push(new AlertResponseEvent(null, "user1", "005930", AlertDirection.BELOW,
                    65_000, null, false, AlertErrorCode.INTERNAL_ERROR));
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message("user:alert.user1:event", "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(pushService);
        }
    }

    @Nested
    @DisplayName("알림 해제 응답")
    class CancelResponse {

        private final AlertCancelResponsePushService pushService = mock(AlertCancelResponsePushService.class);
        private final RedisAlertCancelResponseEventSubscriber subscriber =
                new RedisAlertCancelResponseEventSubscriber(OBJECT_MAPPER, pushService);

        @Test
        @DisplayName("해제 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다")
        void onMessage() {
            subscriber.onMessage(message("user:alert:cancel.user1:event", """
                    {"alertId": 1, "username": "user1", "stockCode": "005930", "direction": "BELOW",
                     "triggerPrice": 65000, "success": false, "errorCode": "ALREADY_FIRED"}
                    """), null);

            ArgumentCaptor<AlertCancelResponseEvent> captor = ArgumentCaptor.forClass(AlertCancelResponseEvent.class);
            verify(pushService).push(captor.capture());

            AlertCancelResponseEvent event = captor.getValue();
            assertThat(event.alertId()).isEqualTo(1L);
            assertThat(event.username()).isEqualTo("user1");
            assertThat(event.direction()).isEqualTo(AlertDirection.BELOW);
            assertThat(event.triggerPrice()).isEqualTo(65_000);
            assertThat(event.success()).isFalse();
            assertThat(event.errorCode()).isEqualTo(AlertCancelErrorCode.ALREADY_FIRED);
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message("user:alert:cancel.user1:event", "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(pushService);
        }
    }

    @Nested
    @DisplayName("알림 발동")
    class Fired {

        private final AlertFiredPushService pushService = mock(AlertFiredPushService.class);
        private final RedisAlertFiredEventSubscriber subscriber =
                new RedisAlertFiredEventSubscriber(OBJECT_MAPPER, pushService);

        @Test
        @DisplayName("발동 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다 (현재가·발동 시각 포함)")
        void onMessage() {
            subscriber.onMessage(message("user:alert:fired.user1:event", """
                    {"alertId": 1, "username": "user1", "stockCode": "005930", "direction": "ABOVE",
                     "triggerPrice": 70000, "currentPrice": 70300, "firedTime": "2026-09-24T01:15:00Z"}
                    """), null);

            verify(pushService).push(new AlertFiredEvent(1L, "user1", "005930", AlertDirection.ABOVE,
                    70_000, 70_300, Instant.parse("2026-09-24T01:15:00Z")));
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message("user:alert:fired.user1:event", "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(pushService);
        }
    }
}
