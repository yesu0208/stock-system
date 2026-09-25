package arile.toy.stocksystem.stockserver.trailingstopcancel.event;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstopcancel.dto.TrailingStopCancelErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Event] 트레일링 스탑 취소 응답 이벤트 변환 테스트")
class TrailingStopCancelResponseEventTest {

    @DisplayName("엔티티 값과 성공 여부·에러 코드로 이벤트를 만든다 (추적 전이면 등록 시점 발동가)")
    @Test
    void givenNotTrailed_whenCreating_thenRegisteredTrigger() {
        TrailingStopEntity entity = entity();

        TrailingStopCancelResponseEvent event =
                TrailingStopCancelResponseEvent.of(entity, false, TrailingStopCancelErrorCode.INTERNAL_ERROR);

        assertThat(event.trailingStopId()).isEqualTo(1L);
        assertThat(event.username()).isEqualTo("user");
        assertThat(event.stockCode()).isEqualTo("005930");
        assertThat(event.trailingStopType()).isEqualTo(TrailingStopType.SELL);
        assertThat(event.triggerPrice()).isEqualTo(67_900);
        assertThat(event.orderQuantity()).isEqualTo(10);
        assertThat(event.success()).isFalse();
        assertThat(event.errorCode()).isEqualTo(TrailingStopCancelErrorCode.INTERNAL_ERROR);
    }

    @DisplayName("추적 중이었다면 현재 발동가를 담는다")
    @Test
    void givenTrailed_whenCreating_thenCurrentTrigger() {
        TrailingStopEntity entity = entity();
        entity.setCurrentTriggerPrice(69_800);

        assertThat(TrailingStopCancelResponseEvent.of(entity, true, null).triggerPrice()).isEqualTo(69_800);
    }

    private TrailingStopEntity entity() {
        TrailingStopEntity entity = TrailingStopEntity.of("user", "005930", TrailingStopType.SELL,
                LeverageRatio.SPOT, 10, 3.0, 70_000, 67_900, TrailingStopStatus.CANCELED);
        entity.setTrailingStopId(1L);
        return entity;
    }
}
