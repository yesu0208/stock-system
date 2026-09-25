package arile.toy.stocksystem.stockserver.order.event.publisher;

import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.event.OrderResponseEvent;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 주문 응답 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisOrderResponseEventPublisherTest {

    private static final String CHANNEL = "user:order.user:event";
    private static final Instant ORDER_TIME = Instant.parse("2026-09-25T00:00:00Z");

    @InjectMocks private RedisOrderResponseEventPublisher sut;

    @Mock private RedisTemplate<String, OrderResponseEvent> redisOrderResponseEventRedisTemplate;

    @DisplayName("주문 성공 응답은 사용자 채널로 success=true, errorCode=null 이벤트를 발행한다")
    @Test
    void givenResponseMessage_whenPublishing_thenSendsSuccessEvent() {
        // Given
        var message = StockServerOrderResponseMessage.of(
                1L, "user", "005930", OrderType.BUY, LeverageRatio.X2,
                70_000, 10, 10, ORDER_TIME, OrderExecutionType.LIMIT,
                OrderOrigin.MANUAL, null);

        // When
        sut.publish(message);

        // Then
        ArgumentCaptor<OrderResponseEvent> captor = ArgumentCaptor.forClass(OrderResponseEvent.class);
        then(redisOrderResponseEventRedisTemplate).should().convertAndSend(eq(CHANNEL), captor.capture());

        OrderResponseEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(1L);
        assertThat(event.username()).isEqualTo("user");
        assertThat(event.stockCode()).isEqualTo("005930");
        assertThat(event.orderType()).isEqualTo(OrderType.BUY);
        assertThat(event.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(event.orderPrice()).isEqualTo(70_000);
        assertThat(event.orderQuantity()).isEqualTo(10);
        assertThat(event.orderTime()).isEqualTo(ORDER_TIME);
        assertThat(event.success()).isTrue();
        assertThat(event.errorCode()).isNull();
    }

    @DisplayName("주문 실패 응답은 주문 ID·시간 없이 success=false와 에러 코드를 담아 발행한다")
    @Test
    void givenRequestAndErrorCode_whenPublishingError_thenSendsFailureEvent() {
        // Given
        var request = new StockServerOrderRequestEvent(
                "user", "005930", OrderType.SELL, 70_000, 10, LeverageRatio.SPOT,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);

        // When
        sut.publishError(request, OrderErrorCode.INSUFFICIENT_STOCK);

        // Then
        ArgumentCaptor<OrderResponseEvent> captor = ArgumentCaptor.forClass(OrderResponseEvent.class);
        then(redisOrderResponseEventRedisTemplate).should().convertAndSend(eq(CHANNEL), captor.capture());

        OrderResponseEvent event = captor.getValue();
        assertThat(event.orderId()).isNull();
        assertThat(event.orderTime()).isNull();
        assertThat(event.username()).isEqualTo("user");
        assertThat(event.stockCode()).isEqualTo("005930");
        assertThat(event.orderType()).isEqualTo(OrderType.SELL);
        assertThat(event.leverageRatio()).isEqualTo(LeverageRatio.SPOT);
        assertThat(event.orderPrice()).isEqualTo(70_000);
        assertThat(event.orderQuantity()).isEqualTo(10);
        assertThat(event.success()).isFalse();
        assertThat(event.errorCode()).isEqualTo(OrderErrorCode.INSUFFICIENT_STOCK);
    }

    @DisplayName("성공 이벤트 발행에 실패해도 예외를 밖으로 던지지 않는다")
    @Test
    void givenSendFails_whenPublishing_thenSwallowsException() {
        // Given
        var message = StockServerOrderResponseMessage.of(
                1L, "user", "005930", OrderType.BUY, LeverageRatio.SPOT,
                70_000, 10, 10, ORDER_TIME, OrderExecutionType.LIMIT,
                OrderOrigin.MANUAL, null);
        given(redisOrderResponseEventRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new IllegalStateException("redis down"));

        // When & Then
        assertThatCode(() -> sut.publish(message)).doesNotThrowAnyException();
    }

    @DisplayName("에러 이벤트 발행에 실패해도 예외를 밖으로 던지지 않는다")
    @Test
    void givenSendFails_whenPublishingError_thenSwallowsException() {
        // Given
        var request = new StockServerOrderRequestEvent(
                "user", "005930", OrderType.BUY, 70_000, 10, null,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
        given(redisOrderResponseEventRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new IllegalStateException("redis down"));

        // When & Then
        assertThatCode(() -> sut.publishError(request, OrderErrorCode.INTERNAL_ERROR))
                .doesNotThrowAnyException();
    }
}
