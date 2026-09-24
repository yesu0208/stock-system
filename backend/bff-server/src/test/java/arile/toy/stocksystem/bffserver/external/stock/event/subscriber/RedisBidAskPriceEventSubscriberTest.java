package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.service.BidAskPricePushService;
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

class RedisBidAskPriceEventSubscriberTest {

    private final BidAskPricePushService bidAskPricePushService = mock(BidAskPricePushService.class);
    private final RedisBidAskPriceEventSubscriber subscriber =
            new RedisBidAskPriceEventSubscriber(new ObjectMapper(), bidAskPricePushService);

    private static Message message(String json) {
        return new DefaultMessage("bidask:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("호가 이벤트의 종목코드로 호가를 푸시한다")
    void onMessage() {
        subscriber.onMessage(message("{\"stockCode\": \"005930\"}"), null);

        verify(bidAskPricePushService).push("005930");
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(bidAskPricePushService);
    }
}
