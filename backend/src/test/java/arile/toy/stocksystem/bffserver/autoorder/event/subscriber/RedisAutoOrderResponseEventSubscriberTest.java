package arile.toy.stocksystem.bffserver.autoorder.event.subscriber;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.service.AutoOrderResponsePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisAutoOrderResponseEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AutoOrderResponsePushService autoOrderResponsePushService;

    @InjectMocks
    private RedisAutoOrderResponseEventSubscriber subscriber;

    @Mock
    private Message message;

    @Test
    @DisplayName("정상 메시지 처리 시 push 호출")
    void givenValidMessage_whenOnMessage_thenPushCalled() throws Exception {
        // given
        Instant orderTime = Instant.ofEpochMilli(50);

        AutoOrderResponseEvent event = new AutoOrderResponseEvent(
                1L,
                "user1",
                "005930",
                AutoOrderType.BUY,
                50000,
                50000,
                50,
                orderTime,
                true,
                null
        );

        String json = "{"
                + "\"autoOrderId\":1,"
                + "\"username\":\"user1\","
                + "\"stockCode\":\"005930\","
                + "\"autoOrderType\":\"BUY\","
                + "\"triggerPrice\":50000,"
                + "\"orderPrice\":50000,"
                + "\"orderQuantity\":50,"
                + "\"orderTime\":\"" + orderTime.toString() + "\","
                + "\"success\":true,"
                + "\"reason\":null"
                + "}";
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(json, AutoOrderResponseEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(autoOrderResponsePushService).push(event);
    }


    @Test
    @DisplayName("잘못된 메시지 처리 시 예외는 던지지 않고 로그만")
    void givenInvalidMessage_whenOnMessage_thenLogsWarning() throws Exception {
        // given
        String invalidJson = "INVALID_JSON";
        byte[] bodyBytes = invalidJson.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(anyString(), eq(AutoOrderResponseEvent.class)))
                .thenThrow(new RuntimeException("parse error"));

        // when
        subscriber.onMessage(message, null);

        // then
        verify(autoOrderResponsePushService, never()).push(any());
    }
}
