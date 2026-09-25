package arile.toy.stocksystem.stockserver.autocancel.event.publisher;

import arile.toy.stocksystem.stockserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
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

@DisplayName("[Publisher] 자동주문 취소 응답 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisAutoCancelResponseEventPublisherTest {

    @InjectMocks private RedisAutoCancelResponseEventPublisher sut;

    @Mock private RedisTemplate<String, AutoCancelResponseEvent> redisAutoCancelResponseEventRedisTemplate;

    @DisplayName("취소 응답을 사용자 채널(user:auto:cancel.{username}:event)로 자동주문 정보와 함께 발행한다")
    @Test
    void whenPublishing_thenSendsToUserChannel() {
        var event = AutoCancelResponseEvent.of(entity(), false, AutoCancelErrorCode.ALREADY_TRIGGERED);

        sut.publish(event);

        ArgumentCaptor<AutoCancelResponseEvent> captor = ArgumentCaptor.forClass(AutoCancelResponseEvent.class);
        then(redisAutoCancelResponseEventRedisTemplate).should()
                .convertAndSend(eq("user:auto:cancel.user:event"), captor.capture());
        AutoCancelResponseEvent sent = captor.getValue();
        assertThat(sent.autoOrderId()).isEqualTo(1L);
        assertThat(sent.autoOrderType()).isEqualTo(AutoOrderType.SELL);
        assertThat(sent.triggerPrice()).isEqualTo(69_000);
        assertThat(sent.orderPrice()).isEqualTo(68_500);
        assertThat(sent.orderQuantity()).isEqualTo(10);
        assertThat(sent.success()).isFalse();
        assertThat(sent.errorCode()).isEqualTo(AutoCancelErrorCode.ALREADY_TRIGGERED);
    }

    @DisplayName("발행에 실패해도 예외를 밖으로 던지지 않는다 (환불 이후 롤백 방지)")
    @Test
    void givenSendFails_whenPublishing_thenSwallows() {
        given(redisAutoCancelResponseEventRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> sut.publish(AutoCancelResponseEvent.of(entity(), true, null)))
                .doesNotThrowAnyException();
    }

    private AutoOrderEntity entity() {
        AutoOrderEntity entity = AutoOrderEntity.of("user", "005930", AutoOrderType.SELL, LeverageRatio.SPOT,
                69_000, 68_500, 10, AutoOrderStatus.ACTIVE);
        entity.setAutoOrderId(1L);
        return entity;
    }
}
