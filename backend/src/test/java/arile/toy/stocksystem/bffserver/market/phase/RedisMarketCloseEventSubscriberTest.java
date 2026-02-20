package arile.toy.stocksystem.bffserver.market.phase;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

@ExtendWith(MockitoExtension.class)
class RedisMarketCloseEventSubscriberTest {

    @Mock
    private MarketCloseEventPushService marketCloseEventPushService;

    @InjectMocks
    private RedisMarketCloseEventSubscriber subscriber;

    @Test
    @DisplayName("Redis 메시지 수신 시 MarketCloseEventPushService 호출")
    void givenRedisMessage_whenOnMessage_thenPushCalled() {
        // given
        String payload = "Market closed";
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));

        // when
        subscriber.onMessage(message, null);

        // then
        verify(marketCloseEventPushService).push(payload);
        verifyNoMoreInteractions(marketCloseEventPushService);
    }

    @Test
    @DisplayName("예외 발생 시 로그 경고 처리 (mocking로 검증 가능)")
    void givenException_whenOnMessage_thenLogsWarning() {
        // given
        Message message = mock(Message.class);
        when(message.getBody()).thenThrow(new RuntimeException("fail"));

        // when
        assertDoesNotThrow(() -> subscriber.onMessage(message, null));

        // then
        verifyNoInteractions(marketCloseEventPushService);
    }
}
