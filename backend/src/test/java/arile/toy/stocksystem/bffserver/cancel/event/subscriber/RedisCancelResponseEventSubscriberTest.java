package arile.toy.stocksystem.bffserver.cancel.event.subscriber;

import arile.toy.stocksystem.bffserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.bffserver.cancel.service.CancelResponsePushService;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCancelResponseEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CancelResponsePushService cancelResponsePushService;

    @InjectMocks
    private RedisCancelResponseEventSubscriber subscriber;

    @Mock
    private Message message;

    @Test
    @DisplayName("정상 메시지 처리 시 push 호출")
    void givenValidMessage_whenOnMessage_thenPushCalled() throws Exception {
        // given
        CancelResponseEvent event = new CancelResponseEvent(
                1L, "user1", "005930", OrderType.BUY,
                50000, 50, true, null
        );

        String json = "{"
                + "\"orderId\":1,"
                + "\"username\":\"user1\","
                + "\"stockCode\":\"005930\","
                + "\"orderType\":\"BUY\","
                + "\"orderPrice\":50000,"
                + "\"orderQuantity\":50,"
                + "\"success\":true,"
                + "\"errorCode\":null"
                + "}";

        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(json, CancelResponseEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(cancelResponsePushService).push(event);
    }

    @Test
    @DisplayName("잘못된 메시지 처리 시 예외는 던지지 않고 로그만")
    void givenInvalidMessage_whenOnMessage_thenLogsWarning() throws Exception {
        // given
        String invalidJson = "INVALID_JSON";
        byte[] bodyBytes = invalidJson.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(anyString(), eq(CancelResponseEvent.class)))
                .thenThrow(new RuntimeException("parse error"));

        // when
        subscriber.onMessage(message, null);

        // then
        verify(cancelResponsePushService, never()).push(any());
    }
}
