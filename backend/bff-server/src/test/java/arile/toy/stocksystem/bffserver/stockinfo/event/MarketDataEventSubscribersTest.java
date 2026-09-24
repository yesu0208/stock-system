package arile.toy.stocksystem.bffserver.stockinfo.event;

import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MarketDataEventSubscribersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Message message(String channel, String json) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("국내 지수")
    class MarketMain {

        private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        private final MarketMainEventSubscriber subscriber =
                new MarketMainEventSubscriber(messagingTemplate, OBJECT_MAPPER);

        @Test
        @DisplayName("지수 이벤트를 /sub/market/main으로 전체 전송한다 (로그인 화면 포함)")
        void onMessage() throws Exception {
            MarketMainResponse response = new MarketMainResponse(null, null, null);

            subscriber.onMessage(message(MarketMainRedisPublisher.CHANNEL,
                    OBJECT_MAPPER.writeValueAsString(response)), null);

            verify(messagingTemplate).convertAndSend("/sub/market/main", response);
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message(MarketMainRedisPublisher.CHANNEL, "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("환율")
    class GlobalMarket {

        private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        private final GlobalMarketEventSubscriber subscriber =
                new GlobalMarketEventSubscriber(messagingTemplate, OBJECT_MAPPER);

        @Test
        @DisplayName("환율 이벤트를 /sub/market/global로 전체 전송한다")
        void onMessage() throws Exception {
            GlobalMarketResponse response = new GlobalMarketResponse(List.of());

            subscriber.onMessage(message(GlobalMarketRedisPublisher.CHANNEL,
                    OBJECT_MAPPER.writeValueAsString(response)), null);

            verify(messagingTemplate).convertAndSend("/sub/market/global", response);
        }

        @Test
        @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
        void brokenJson_ignored() {
            assertThatCode(() -> subscriber.onMessage(message(GlobalMarketRedisPublisher.CHANNEL, "{broken"), null))
                    .doesNotThrowAnyException();

            verifyNoInteractions(messagingTemplate);
        }
    }
}
