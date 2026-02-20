package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.bffserver.external.stock.service.BidAskPricePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisBidAskPriceEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BidAskPricePushService bidAskPricePushService;

    @Mock
    private Message message;

    @InjectMocks
    private RedisBidAskPriceEventSubscriber subscriber;

    @Test
    @DisplayName("정상 메시지 수신 시 push 호출")
    void givenValidMessage_whenOnMessage_thenPushCalled() throws Exception {
        // given
        String stockCode = "005930";
        BidAskPriceTickEvent event = new BidAskPriceTickEvent(stockCode);

        String json = "{\"stockCode\":\"" + stockCode + "\"}";
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(json, BidAskPriceTickEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(bidAskPricePushService).push(stockCode);
    }

    @Test
    @DisplayName("메시지 파싱 실패 시 예외 로그만 발생")
    void givenInvalidMessage_whenOnMessage_thenLogsWarning() throws Exception {
        // given
        byte[] bodyBytes = "INVALID_JSON".getBytes(StandardCharsets.UTF_8);
        when(message.getBody()).thenReturn(bodyBytes);
        when(objectMapper.readValue(any(byte[].class), eq(BidAskPriceTickEvent.class)))
                .thenThrow(new RuntimeException("Parsing error"));

        // when
        subscriber.onMessage(message, null);

        // then
        verifyNoInteractions(bidAskPricePushService);
    }
}
