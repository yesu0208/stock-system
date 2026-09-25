package arile.toy.stocksystem.stockserver.trailingstop.event.publisher;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.*;
import arile.toy.stocksystem.stockserver.trailingstop.event.StockServerTrailingStopRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstop.event.TrailingStopResponseEvent;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 트레일링 스탑 응답 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisTrailingStopResponseEventPublisherTest {

    private static final String CHANNEL = "user:trailing:stop.user:event";
    private static final Instant ORDER_TIME = Instant.parse("2026-09-25T00:00:00Z");

    @InjectMocks private RedisTrailingStopResponseEventPublisher sut;
    @Mock private RedisTemplate<String, TrailingStopResponseEvent> redisTemplate;

    @DisplayName("등록 응답: 메시지 전체를 성공 이벤트로 발행한다")
    @Test
    void whenPublishing_thenSendsSuccess() {
        var message = new StockServerTrailingStopResponseMessage(1L, "user", "005930", TrailingStopType.BUY,
                LeverageRatio.SPOT, 10, 3.0, 70_000, 72_100, ORDER_TIME);

        sut.publish(message);

        TrailingStopResponseEvent event = captured();
        assertThat(event.trailingStopId()).isEqualTo(1L);
        assertThat(event.triggerPrice()).isEqualTo(72_100);
        assertThat(event.orderTime()).isEqualTo(ORDER_TIME);
        assertThat(event.success()).isTrue();
        assertThat(event.resultCode()).isNull();
    }

    @DisplayName("등록 실패: 요청 값과 결과 코드로 실패 이벤트를 발행한다")
    @Test
    void whenPublishingError_thenSendsFailure() {
        var request = StockServerTrailingStopRequestEvent.of("user", "005930", TrailingStopType.SELL,
                10, 3.0, 70_000, LeverageRatio.X2);

        sut.publishError(request, TrailingStopResultCode.INSUFFICIENT_STOCK);

        TrailingStopResponseEvent event = captured();
        assertThat(event.trailingStopId()).isNull();
        assertThat(event.basePrice()).isEqualTo(70_000);
        assertThat(event.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(event.success()).isFalse();
        assertThat(event.resultCode()).isEqualTo(TrailingStopResultCode.INSUFFICIENT_STOCK);
    }

    @DisplayName("발동: 사용자명과 TRIGGERED만 담아 발행한다")
    @Test
    void whenPublishingTrigger_thenSendsTriggered() {
        sut.publishTrigger("user");

        TrailingStopResponseEvent event = captured();
        assertThat(event.username()).isEqualTo("user");
        assertThat(event.stockCode()).isNull();
        assertThat(event.success()).isTrue();
        assertThat(event.resultCode()).isEqualTo(TrailingStopResultCode.TRIGGERED);
    }

    @DisplayName("발동 실패: 현재 발동가를 담아 실패 이벤트를 발행한다")
    @Test
    void whenPublishingTriggerFailure_thenSendsFailure() {
        sut.publishTriggerFailure(dto(), TrailingStopResultCode.INTERNAL_ERROR);

        TrailingStopResponseEvent event = captured();
        assertThat(event.trailingStopId()).isEqualTo(1L);
        assertThat(event.triggerPrice()).isEqualTo(69_800);
        assertThat(event.orderTime()).isNull();
        assertThat(event.success()).isFalse();
        assertThat(event.resultCode()).isEqualTo(TrailingStopResultCode.INTERNAL_ERROR);
    }

    @DisplayName("추적 갱신: 갱신된 기준가·발동가를 TRAILING_UPDATED로 발행한다")
    @Test
    void whenPublishingTrailingUpdate_thenSendsUpdated() {
        sut.publishTrailingUpdate(dto());

        TrailingStopResponseEvent event = captured();
        assertThat(event.basePrice()).isEqualTo(72_000);
        assertThat(event.triggerPrice()).isEqualTo(69_800);
        assertThat(event.orderTime()).isEqualTo(ORDER_TIME);
        assertThat(event.success()).isTrue();
        assertThat(event.resultCode()).isEqualTo(TrailingStopResultCode.TRAILING_UPDATED);
    }

    @DisplayName("Redis 발행이 실패해도 예외를 전파하지 않는다")
    @Test
    void givenRedisFails_whenPublishing_thenSwallows() {
        given(redisTemplate.convertAndSend(anyString(), any(TrailingStopResponseEvent.class)))
                .willThrow(new IllegalStateException("redis down"));

        assertThatNoException().isThrownBy(() -> {
            sut.publishTrigger("user");
            sut.publishTrailingUpdate(dto());
            sut.publishTriggerFailure(dto(), TrailingStopResultCode.INTERNAL_ERROR);
            sut.publishError(StockServerTrailingStopRequestEvent.of("user", "005930", TrailingStopType.BUY,
                    10, 3.0, 70_000, null), TrailingStopResultCode.INSUFFICIENT_BALANCE);
            sut.publish(StockServerTrailingStopResponseMessage.fromDto(dto()));
        });
    }

    private TrailingStopResponseEvent captured() {
        ArgumentCaptor<TrailingStopResponseEvent> captor = ArgumentCaptor.forClass(TrailingStopResponseEvent.class);
        then(redisTemplate).should().convertAndSend(eq(CHANNEL), captor.capture());
        return captor.getValue();
    }

    private TrailingStopDto dto() {
        return new TrailingStopDto(1L, "user", "005930", TrailingStopType.SELL, LeverageRatio.SPOT,
                10, 3.0, 72_000, 69_800, 67_900, TrailingStopStatus.ACTIVE, ORDER_TIME);
    }
}
