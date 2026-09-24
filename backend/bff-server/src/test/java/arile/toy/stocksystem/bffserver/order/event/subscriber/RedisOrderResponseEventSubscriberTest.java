package arile.toy.stocksystem.bffserver.order.event.subscriber;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.order.dto.OrderErrorCode;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.bffserver.order.service.OrderResponsePushService;
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

class RedisOrderResponseEventSubscriberTest {

    private final OrderResponsePushService pushService = mock(OrderResponsePushService.class);
    private final RedisOrderResponseEventSubscriber subscriber =
            new RedisOrderResponseEventSubscriber(new ObjectMapper().findAndRegisterModules(), pushService);

    private static Message message(String json) {
        return new DefaultMessage("user:order.user1:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("주문 응답 메시지를 이벤트로 바꿔 푸시 서비스에 넘긴다 (접수 시각·오류 코드 포함)")
    void onMessage() {
        subscriber.onMessage(message("""
                {"orderId": 1, "username": "user1", "stockCode": "005930", "orderType": "SELL",
                 "leverageRatio": "SPOT", "orderPrice": 70000, "orderQuantity": 10,
                 "orderTime": "2026-09-24T00:30:00Z", "success": false, "errorCode": "INSUFFICIENT_STOCK"}
                """), null);

        verify(pushService).push(new OrderResponseEvent(1L, "user1", "005930", OrderType.SELL, LeverageRatio.SPOT,
                70_000, 10, Instant.parse("2026-09-24T00:30:00Z"), false, OrderErrorCode.INSUFFICIENT_STOCK));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(pushService);
    }
}
