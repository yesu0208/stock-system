package arile.toy.stocksystem.stockserver.cancel.event.publisher;

import arile.toy.stocksystem.stockserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 취소 응답 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisCancelResponseEventPublisherTest {

    @InjectMocks private RedisCancelResponseEventPublisher sut;

    @Mock private RedisTemplate<String, CancelResponseEvent> redisCancelResponseEventRedisTemplate;

    @DisplayName("취소 성공 응답을 주문자별 채널로 발행하고, 주문 정보를 그대로 담는다")
    @Test
    void givenSuccessEvent_whenPublishing_thenSendsToUserChannel() {
        // Given
        var event = CancelResponseEvent.of(order(), true, null);

        // When
        sut.publish(event);

        // Then
        ArgumentCaptor<CancelResponseEvent> captor = ArgumentCaptor.forClass(CancelResponseEvent.class);
        then(redisCancelResponseEventRedisTemplate).should()
                .convertAndSend(eq("user:cancel.user:event"), captor.capture());

        CancelResponseEvent sent = captor.getValue();
        assertThat(sent.orderId()).isEqualTo(1L);
        assertThat(sent.username()).isEqualTo("user");
        assertThat(sent.stockCode()).isEqualTo("005930");
        assertThat(sent.orderType()).isEqualTo(OrderType.BUY);
        assertThat(sent.orderPrice()).isEqualTo(70_000);
        assertThat(sent.orderQuantity()).isEqualTo(10);
        assertThat(sent.success()).isTrue();
        assertThat(sent.errorCode()).isNull();
    }

    @DisplayName("취소 실패 응답은 success=false와 에러 코드를 담아 발행한다")
    @Test
    void givenFailureEvent_whenPublishing_thenSendsErrorCode() {
        // Given
        var event = CancelResponseEvent.of(order(), false, CancelErrorCode.ALREADY_FILLED);

        // When
        sut.publish(event);

        // Then
        ArgumentCaptor<CancelResponseEvent> captor = ArgumentCaptor.forClass(CancelResponseEvent.class);
        then(redisCancelResponseEventRedisTemplate).should()
                .convertAndSend(eq("user:cancel.user:event"), captor.capture());
        assertThat(captor.getValue().success()).isFalse();
        assertThat(captor.getValue().errorCode()).isEqualTo(CancelErrorCode.ALREADY_FILLED);
    }

    @DisplayName("발행에 실패해도 예외를 밖으로 던지지 않는다 (환불 이후 롤백 방지)")
    @Test
    void givenSendFails_whenPublishing_thenSwallowsException() {
        // Given
        var event = CancelResponseEvent.of(order(), true, null);
        given(redisCancelResponseEventRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new IllegalStateException("redis down"));

        // When & Then
        assertThatCode(() -> sut.publish(event)).doesNotThrowAnyException();
    }

    private OrderEntity order() {
        OrderEntity entity = OrderEntity.of(
                "user", "005930", OrderType.BUY, LeverageRatio.SPOT,
                70_000, 10, OrderStatus.CANCELED, 4,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null, 42L, null);
        entity.setOrderId(1L);
        return entity;
    }
}
