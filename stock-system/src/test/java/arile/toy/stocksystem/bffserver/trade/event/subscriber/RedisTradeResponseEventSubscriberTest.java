package arile.toy.stocksystem.bffserver.trade.event.subscriber;

import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.bffserver.trade.service.TradeResponsePushService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTradeResponseEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TradeResponsePushService tradeResponsePushService;

    private RedisTradeResponseEventSubscriber subscriber;

    @BeforeEach
    void setup() {
        subscriber = new RedisTradeResponseEventSubscriber(objectMapper, tradeResponsePushService);
    }

    @Test
    @DisplayName("유효한 메시지를 받으면 TradeResponsePushService의 push 호출")
    void givenValidMessage_whenOnMessage_thenPushCalled() throws Exception {
        // Given
        String json = "{\"tradeId\":123,\"username\":\"user1\"}";
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        TradeResponseEvent event = mock(TradeResponseEvent.class);
        when(objectMapper.readValue(json, TradeResponseEvent.class)).thenReturn(event);

        // When
        subscriber.onMessage(message, null);

        // Then
        verify(objectMapper).readValue(json, TradeResponseEvent.class);
        verify(tradeResponsePushService).push(event);
    }

    @Test
    @DisplayName("잘못된 메시지를 받으면 경고 로그만 기록하고 push는 호출하지 않음")
    void givenInvalidMessage_whenOnMessage_thenLogsWarning() throws Exception {
        // Given
        String invalidJson = "INVALID_JSON";
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(invalidJson.getBytes(StandardCharsets.UTF_8));

        when(objectMapper.readValue(invalidJson, TradeResponseEvent.class))
                .thenThrow(new JsonProcessingException("error") {
                });

        // When
        subscriber.onMessage(message, null);

        // Then
        verify(objectMapper).readValue(invalidJson, TradeResponseEvent.class);
        verifyNoInteractions(tradeResponsePushService);
    }
}
