package arile.toy.stocksystem.bffserver.trade.event.subscriber;

import arile.toy.stocksystem.bffserver.trade.dto.TradeType;
import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.bffserver.trade.service.TradeResponsePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisTradeResponseEventSubscriberTest {

    private final TradeResponsePushService pushService = mock(TradeResponsePushService.class);
    private final RedisTradeResponseEventSubscriber subscriber =
            new RedisTradeResponseEventSubscriber(new ObjectMapper().findAndRegisterModules(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:trade.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("체결 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다 (체결 시각 포함)")
    void onMessage() {
        subscriber.onMessage(message("""
                {"tradeId": 100, "orderId": 1, "username": "user1", "stockCode": "005930", "tradeType": "BUY",
                 "tradePrice": 70000, "tradeQuantity": 3, "executedAt": "2026-09-24T00:31:00Z"}
                """), null);

        verify(pushService).push(new TradeResponseEvent(100L, 1L, "user1", "005930", TradeType.BUY,
                70_000, 3, Instant.parse("2026-09-24T00:31:00Z")));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
