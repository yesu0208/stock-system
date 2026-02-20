package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.event.StockSummaryTickEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisStockSummaryEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Message message;

    @InjectMocks
    private RedisStockSummaryEventSubscriber subscriber;

    @Test
    @DisplayName("정상 메시지 처리 시 이벤트 발행")
    void givenValidMessage_whenOnMessage_thenEventPublished() throws Exception {
        // given
        String stockCode = "005930";
        StockSummaryTickEvent event = new StockSummaryTickEvent(stockCode);
        String json = "{\"stockCode\":\"" + stockCode + "\"}";
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(json, StockSummaryTickEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(eventPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("잘못된 메시지 처리 시 예외는 던지지 않고 로그만")
    void givenInvalidMessage_whenOnMessage_thenLogsWarning() throws Exception {
        // given
        String invalidJson = "INVALID_JSON";
        byte[] bodyBytes = invalidJson.getBytes(StandardCharsets.UTF_8);
        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(invalidJson, StockSummaryTickEvent.class))
                .thenThrow(new RuntimeException("parse error"));

        // when
        subscriber.onMessage(message, null);

        // then
        verifyNoInteractions(eventPublisher);
    }
}
