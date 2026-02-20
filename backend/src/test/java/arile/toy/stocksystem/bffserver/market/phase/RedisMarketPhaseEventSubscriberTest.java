package arile.toy.stocksystem.bffserver.market.phase;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

@ExtendWith(MockitoExtension.class)
class RedisMarketPhaseEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BffServerMarketPhaseRegistry registry;

    @InjectMocks
    private RedisMarketPhaseEventSubscriber subscriber;

    @Mock
    private Message message;

    @Test
    @DisplayName("MarketPhaseEvent 수신 시 Registry OPEN 호출")
    void givenOpenEvent_whenOnMessage_thenRegistrySetOpen() throws Exception {
        // given
        String stockCode = "005930";
        MarketPhaseEvent event = new MarketPhaseEvent(stockCode, BffServerMarketPhase.OPEN);
        String json = "{\"stockCode\":\"005930\",\"marketPhase\":\"OPEN\"}";

        when(message.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
        when(objectMapper.readValue(json, MarketPhaseEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(registry).setOpen(stockCode);
        verify(registry, never()).setClosed(anyString());
    }

    @Test
    @DisplayName("MarketPhaseEvent 수신 시 Registry CLOSED 호출")
    void givenClosedEvent_whenOnMessage_thenRegistrySetClosed() throws Exception {
        // given
        String stockCode = "005930";
        MarketPhaseEvent event = new MarketPhaseEvent(stockCode, BffServerMarketPhase.CLOSED);
        String json = "{\"stockCode\":\"005930\",\"marketPhase\":\"CLOSED\"}";

        when(message.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
        when(objectMapper.readValue(json, MarketPhaseEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(registry).setClosed(stockCode);
        verify(registry, never()).setOpen(anyString());
    }

    @Test
    @DisplayName("ObjectMapper 예외 발생 시 push 호출 없음")
    void givenInvalidJson_whenOnMessage_thenNoRegistryCall() throws Exception {
        // given
        when(message.getBody()).thenReturn("INVALID_JSON".getBytes(StandardCharsets.UTF_8));
        when(objectMapper.readValue(anyString(), eq(MarketPhaseEvent.class)))
                .thenThrow(new RuntimeException("fail"));

        // when
        assertDoesNotThrow(() -> subscriber.onMessage(message, null));

        // then
        verifyNoInteractions(registry);
    }
}
