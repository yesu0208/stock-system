package arile.toy.stocksystem.bffserver.order.event.subscriber;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.bffserver.order.service.OrderResponsePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

@ExtendWith(MockitoExtension.class)
class RedisOrderResponseEventSubscriberTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderResponsePushService orderResponsePushService;

    @InjectMocks
    private RedisOrderResponseEventSubscriber subscriber;

    @Mock
    private Message message;

    @Test
    @DisplayName("OrderResponseEvent 수신 시 Service 호출")
    void givenValidEvent_whenOnMessage_thenPushCalled() throws Exception {
        // given
        String json = "{\"orderId\":1,\"userId\":\"user1\"}";
        OrderResponseEvent event = new OrderResponseEvent(1L, "user1", "005930",
                OrderType.BUY, 50000, 50, Instant.now(), true, null);

        when(message.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
        when(objectMapper.readValue(json, OrderResponseEvent.class)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(orderResponsePushService).push(event);
        verifyNoMoreInteractions(orderResponsePushService);
    }

    @Test
    @DisplayName("ObjectMapper 예외 발생 시 Service 호출 없음")
    void givenInvalidJson_whenOnMessage_thenNoServiceCall() throws Exception {
        // given
        when(message.getBody()).thenReturn("INVALID_JSON".getBytes(StandardCharsets.UTF_8));
        when(objectMapper.readValue(anyString(), eq(OrderResponseEvent.class)))
                .thenThrow(new RuntimeException("fail"));

        // when
        assertDoesNotThrow(() -> subscriber.onMessage(message, null));

        // then
        verifyNoInteractions(orderResponsePushService);
    }
}
