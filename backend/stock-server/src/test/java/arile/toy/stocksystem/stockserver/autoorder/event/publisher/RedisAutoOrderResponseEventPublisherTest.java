package arile.toy.stocksystem.stockserver.autoorder.event.publisher;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
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

@DisplayName("[Publisher] 자동주문 응답 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisAutoOrderResponseEventPublisherTest {

    private static final String CHANNEL = "user:auto:order.user:event";
    private static final Instant ORDER_TIME = Instant.parse("2026-09-25T00:00:00Z");

    @InjectMocks private RedisAutoOrderResponseEventPublisher sut;

    @Mock private RedisTemplate<String, AutoOrderResponseEvent> redisAutoOrderResponseEventRedisTemplate;

    @DisplayName("등록 성공 응답은 자동주문 정보를 담아 success=true로 발행한다")
    @Test
    void givenResponseMessage_whenPublishing_thenSendsSuccess() {
        var message = new StockServerAutoOrderResponseMessage(1L, "user", "005930", AutoOrderType.BUY,
                LeverageRatio.X2, 71_000, 70_000, 10, ORDER_TIME);

        sut.publish(message);

        AutoOrderResponseEvent sent = captureSent();
        assertThat(sent.autoOrderId()).isEqualTo(1L);
        assertThat(sent.stockCode()).isEqualTo("005930");
        assertThat(sent.autoOrderType()).isEqualTo(AutoOrderType.BUY);
        assertThat(sent.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(sent.triggerPrice()).isEqualTo(71_000);
        assertThat(sent.orderPrice()).isEqualTo(70_000);
        assertThat(sent.orderQuantity()).isEqualTo(10);
        assertThat(sent.orderTime()).isEqualTo(ORDER_TIME);
        assertThat(sent.success()).isTrue();
        assertThat(sent.resultCode()).isNull();
    }

    @DisplayName("등록 실패 응답은 요청 정보와 결과 코드를 담아 success=false로 발행한다")
    @Test
    void givenRequest_whenPublishingError_thenSendsFailure() {
        var request = StockServerAutoOrderRequestEvent.of("user", "005930", AutoOrderType.SELL,
                69_000, 68_500, 5, LeverageRatio.SPOT);

        sut.publishError(request, AutoOrderResultCode.INSUFFICIENT_STOCK);

        AutoOrderResponseEvent sent = captureSent();
        assertThat(sent.autoOrderId()).isNull();
        assertThat(sent.orderTime()).isNull();
        assertThat(sent.autoOrderType()).isEqualTo(AutoOrderType.SELL);
        assertThat(sent.triggerPrice()).isEqualTo(69_000);
        assertThat(sent.orderQuantity()).isEqualTo(5);
        assertThat(sent.success()).isFalse();
        assertThat(sent.resultCode()).isEqualTo(AutoOrderResultCode.INSUFFICIENT_STOCK);
    }

    @DisplayName("발동 알림은 사용자 이름과 TRIGGERED 결과 코드만 담아 발행한다")
    @Test
    void givenUsername_whenPublishingTrigger_thenSendsTriggered() {
        sut.publishTrigger("user");

        AutoOrderResponseEvent sent = captureSent();
        assertThat(sent.username()).isEqualTo("user");
        assertThat(sent.autoOrderId()).isNull();
        assertThat(sent.stockCode()).isNull();
        assertThat(sent.success()).isTrue();
        assertThat(sent.resultCode()).isEqualTo(AutoOrderResultCode.TRIGGERED);
    }

    @DisplayName("발동 실패 알림은 자동주문 정보와 결과 코드를 담아 success=false로 발행한다")
    @Test
    void givenDto_whenPublishingTriggerFailure_thenSendsFailure() {
        var dto = new AutoOrderDto(1L, "user", "005930", AutoOrderType.BUY, LeverageRatio.SPOT,
                71_000, 70_000, 10, ORDER_TIME);

        sut.publishTriggerFailure(dto, AutoOrderResultCode.INTERNAL_ERROR);

        AutoOrderResponseEvent sent = captureSent();
        assertThat(sent.autoOrderId()).isEqualTo(1L);
        assertThat(sent.triggerPrice()).isEqualTo(71_000);
        assertThat(sent.success()).isFalse();
        assertThat(sent.resultCode()).isEqualTo(AutoOrderResultCode.INTERNAL_ERROR);
    }

    @DisplayName("모든 발행 메서드는 Redis 실패 시 예외를 밖으로 던지지 않는다")
    @Test
    void givenSendFails_whenPublishingAny_thenSwallows() {
        given(redisAutoOrderResponseEventRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new IllegalStateException("redis down"));
        var message = new StockServerAutoOrderResponseMessage(1L, "user", "005930", AutoOrderType.BUY,
                LeverageRatio.SPOT, 71_000, 70_000, 10, ORDER_TIME);
        var request = StockServerAutoOrderRequestEvent.of("user", "005930", AutoOrderType.BUY,
                71_000, 70_000, 10, LeverageRatio.SPOT);
        var dto = new AutoOrderDto(1L, "user", "005930", AutoOrderType.BUY, LeverageRatio.SPOT,
                71_000, 70_000, 10, ORDER_TIME);

        assertThatCode(() -> sut.publish(message)).doesNotThrowAnyException();
        assertThatCode(() -> sut.publishError(request, AutoOrderResultCode.INTERNAL_ERROR)).doesNotThrowAnyException();
        assertThatCode(() -> sut.publishTrigger("user")).doesNotThrowAnyException();
        assertThatCode(() -> sut.publishTriggerFailure(dto, AutoOrderResultCode.INTERNAL_ERROR)).doesNotThrowAnyException();
    }

    private AutoOrderResponseEvent captureSent() {
        ArgumentCaptor<AutoOrderResponseEvent> captor = ArgumentCaptor.forClass(AutoOrderResponseEvent.class);
        then(redisAutoOrderResponseEventRedisTemplate).should().convertAndSend(eq(CHANNEL), captor.capture());
        return captor.getValue();
    }
}
