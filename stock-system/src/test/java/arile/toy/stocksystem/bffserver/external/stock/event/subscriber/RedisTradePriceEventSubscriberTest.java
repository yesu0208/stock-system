package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.event.TradePriceTickEvent;
import arile.toy.stocksystem.bffserver.external.stock.service.TradePricePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTradePriceEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TradePricePushService tradePricePushService;

    @Mock
    private Message message;

    @InjectMocks
    private RedisTradePriceEventSubscriber subscriber;

    private final String stockCode = "005930";

    @Test
    @DisplayName("정상 메시지 처리 시 push 호출")
    void givenValidMessage_whenOnMessage_thenPushCalled() throws Exception {
        // given
        TradePriceTickEvent event = new TradePriceTickEvent(stockCode);
        String json = "{\"stockCode\":\"" + stockCode + "\"}";
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(json, TradePriceTickEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(tradePricePushService).push(stockCode);
    }

    @Test
    @DisplayName("잘못된 메시지 처리 시 예외는 던지지 않고 로그만")
    void givenInvalidMessage_whenOnMessage_thenLogsWarning() throws Exception {
        // given
        String invalidJson = "INVALID_JSON";
        byte[] bodyBytes = invalidJson.getBytes(StandardCharsets.UTF_8);
        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(invalidJson, TradePriceTickEvent.class))
                .thenThrow(new RuntimeException("parse error"));

        // when
        subscriber.onMessage(message, null);

        // then
        verifyNoInteractions(tradePricePushService);
    }
}
