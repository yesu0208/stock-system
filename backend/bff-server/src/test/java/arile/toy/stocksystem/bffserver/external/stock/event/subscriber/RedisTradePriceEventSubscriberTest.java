package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.service.TradePricePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisTradePriceEventSubscriberTest {

    private final TradePricePushService tradePricePushService = mock(TradePricePushService.class);
    private final RedisTradePriceEventSubscriber subscriber =
            new RedisTradePriceEventSubscriber(new ObjectMapper(), tradePricePushService);

    private static Message message(String json) {
        return new DefaultMessage("trade:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("체결가 이벤트의 종목코드로 체결가를 푸시한다")
    void onMessage() {
        subscriber.onMessage(message("{\"stockCode\": \"005930\"}"), null);

        verify(tradePricePushService).push("005930");
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(tradePricePushService);
    }
}
