package arile.toy.stocksystem.bffserver.chart.event.subscriber;

import arile.toy.stocksystem.bffserver.chart.dto.DailyCandleTickMessage;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandleTickMessage;
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

class RedisCandleEventSubscribersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Message message(String channel, String json) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("일봉")
    class Daily {

        private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        private final RedisDailyCandleEventSubscriber subscriber =
                new RedisDailyCandleEventSubscriber(OBJECT_MAPPER, messagingTemplate);

        @Test
        @DisplayName("갱신된 일봉을 DAILY_CANDLE 메시지로 감싸 종목 채널로 보낸다")
        void onMessage() throws Exception {
            subscriber.onMessage(message("dailycandle.005930:event", """
                    {"stockCode": "005930",
                     "candle": {"date": "2026-09-24", "open": 70000, "high": 71500,
                                "low": 69800, "close": 71000, "volume": 1000}}
                    """), null);

            ArgumentCaptor<DailyCandleTickMessage> captor = ArgumentCaptor.forClass(DailyCandleTickMessage.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/stock/005930"), captor.capture());

            String json = OBJECT_MAPPER.writeValueAsString(captor.getValue());
            assertThat(json).contains("\"DAILY_CANDLE\"", "\"005930\"", "\"2026-09-24\"", "71500");
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message("dailycandle.005930:event", "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("분봉")
    class Minute {

        private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        private final RedisMinuteCandleEventSubscriber subscriber =
                new RedisMinuteCandleEventSubscriber(OBJECT_MAPPER, messagingTemplate);

        @Test
        @DisplayName("갱신된 분봉을 MINUTE_CANDLE 메시지로 감싸 종목 채널로 보낸다")
        void onMessage() throws Exception {
            subscriber.onMessage(message("minutecandle.005930:event", """
                    {"stockCode": "005930",
                     "candle": {"date": "2026-09-24", "time": "0931", "open": 70000, "high": 70200,
                                "low": 69900, "close": 70100, "volume": 50}}
                    """), null);

            ArgumentCaptor<MinuteCandleTickMessage> captor = ArgumentCaptor.forClass(MinuteCandleTickMessage.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/stock/005930"), captor.capture());

            String json = OBJECT_MAPPER.writeValueAsString(captor.getValue());
            assertThat(json).contains("\"MINUTE_CANDLE\"", "\"005930\"", "\"0931\"", "70100");
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message("minutecandle.005930:event", "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }
}
