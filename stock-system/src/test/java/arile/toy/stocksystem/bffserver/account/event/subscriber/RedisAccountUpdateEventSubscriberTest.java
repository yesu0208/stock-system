package arile.toy.stocksystem.bffserver.account.event.subscriber;

import arile.toy.stocksystem.bffserver.account.event.AccountUpdateEvent;
import arile.toy.stocksystem.bffserver.account.service.AccountPushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisAccountUpdateEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AccountPushService accountPushService;

    @InjectMocks
    private RedisAccountUpdateEventSubscriber subscriber;

    @Mock
    private Message message;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("정상 메시지 처리 시 AccountPushService 호출")
    void givenValidMessage_whenOnMessage_thenPushCalled() throws Exception {
        String username = "user1";
        String json = "{\"username\":\"user1\"}";
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        AccountUpdateEvent event = new AccountUpdateEvent(username);
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(event);

        subscriber.onMessage(message, null);

        verify(accountPushService).push(username);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("잘못된 메시지 처리 시 예외는 던지지 않고 로그만")
    void givenInvalidMessage_whenOnMessage_thenLogsWarning() throws Exception {
        String invalidJson = "INVALID_JSON";
        byte[] bodyBytes = invalidJson.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("parse error"));

        subscriber.onMessage(message, null);

        verify(accountPushService, never()).push(anyString());
    }
}
