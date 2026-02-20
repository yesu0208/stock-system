package arile.toy.stocksystem.stockserver.order.event.publisher;

import arile.toy.stocksystem.stockserver.order.dto.OrderErrorCode;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisOrderResponseEventPublisherTest {

    @Mock
    private RedisTemplate<String, OrderResponseEvent> redisTemplate;

    @InjectMocks
    private RedisOrderResponseEventPublisher publisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<OrderResponseEvent> eventCaptor;

    @Test
    @DisplayName("Order 응답 이벤트를 Redis Pub/Sub으로 발행한다")
    void givenOrderResponse_whenPublish_thenSendsToRedis() {

        // given
        StockServerOrderResponseMessage responseMessage = new StockServerOrderResponseMessage(
                1L, "user1", "005930", OrderType.BUY,
                50000, 50, 50, Instant.now()
        );

        OrderResponseEvent event =
                OrderResponseEvent.fromOrderResponseMessage(responseMessage, true, null);

        // when
        publisher.publish(responseMessage);

        // then
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), eventCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("user:order.user1:event");
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("Order 오류 응답 이벤트를 Redis Pub/Sub으로 발행한다")
    void givenOrderRequestAndError_whenPublishError_thenSendsToRedis() {

        // given
        StockServerOrderRequestEvent requestEvent = new StockServerOrderRequestEvent(
                "user1", "005930", OrderType.BUY, 70000, 10
        );

        OrderErrorCode errorCode = OrderErrorCode.INTERNAL_ERROR;

        OrderResponseEvent event = OrderResponseEvent.of(
                null, requestEvent.username(), requestEvent.stockCode(),
                requestEvent.orderType(), requestEvent.orderPrice(),
                requestEvent.orderQuantity(), null, false,
                errorCode
        );

        // when
        publisher.publishError(requestEvent, errorCode);

        // then
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), eventCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("user:order.user1:event");
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("Redis 예외 발생 시에도 예외를 던지지 않는다")
    void givenRedisThrowsException_whenPublishOrPublishError_thenDoesNotThrow() {

        StockServerOrderResponseMessage responseMessage = new StockServerOrderResponseMessage(
                1L, "user1", "005930", OrderType.BUY,
                50000, 50, 50, Instant.now()
        );

        doThrow(new RuntimeException("Redis error"))
                .when(redisTemplate)
                .convertAndSend(anyString(), any());

        // when & then
        assertDoesNotThrow(() -> publisher.publish(responseMessage));

        StockServerOrderRequestEvent requestEvent = new StockServerOrderRequestEvent(
                "user1", "005930", OrderType.BUY, 70000, 10
        );

        OrderErrorCode errorCode = OrderErrorCode.INTERNAL_ERROR;

        assertDoesNotThrow(() -> publisher.publishError(requestEvent, errorCode));
    }
}
