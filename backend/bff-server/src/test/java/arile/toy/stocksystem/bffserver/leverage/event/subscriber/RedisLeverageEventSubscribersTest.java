package arile.toy.stocksystem.bffserver.leverage.event.subscriber;

import arile.toy.stocksystem.bffserver.leverage.event.LiquidationExecutedEvent;
import arile.toy.stocksystem.bffserver.leverage.event.MarginCallEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisLeverageEventSubscribersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static Message message(String channel, String json) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("마진콜")
    class MarginCall {

        private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        private final RedisMarginCallEventSubscriber subscriber =
                new RedisMarginCallEventSubscriber(OBJECT_MAPPER, messagingTemplate);

        @Test
        @DisplayName("마진콜 이벤트를 해당 사용자의 /sub/margincall 채널로 그대로 전달한다")
        void onMessage() {
            subscriber.onMessage(message("user:margincall.user1:event", """
                    {"username": "user1", "stockCode": "005930"}
                    """), null);

            ArgumentCaptor<MarginCallEvent> captor = ArgumentCaptor.forClass(MarginCallEvent.class);
            verify(messagingTemplate).convertAndSendToUser(eq("user1"), eq("/sub/margincall"), captor.capture());
            assertThat(captor.getValue().username()).isEqualTo("user1");
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message("user:margincall.user1:event", "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("반대매매")
    class Liquidation {

        private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        private final RedisLiquidationEventSubscriber subscriber =
                new RedisLiquidationEventSubscriber(OBJECT_MAPPER, messagingTemplate);

        @Test
        @DisplayName("반대매매 체결 이벤트를 해당 사용자의 /sub/liquidation 채널로 그대로 전달한다")
        void onMessage() {
            subscriber.onMessage(message("user:liquidation.user1:event", """
                    {"username": "user1", "stockCode": "005930"}
                    """), null);

            ArgumentCaptor<LiquidationExecutedEvent> captor = ArgumentCaptor.forClass(LiquidationExecutedEvent.class);
            verify(messagingTemplate).convertAndSendToUser(eq("user1"), eq("/sub/liquidation"), captor.capture());
            assertThat(captor.getValue().username()).isEqualTo("user1");
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message("user:liquidation.user1:event", "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }
}
