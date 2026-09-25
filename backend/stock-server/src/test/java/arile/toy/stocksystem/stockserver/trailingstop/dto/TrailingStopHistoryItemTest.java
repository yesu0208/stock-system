package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 트레일링 스탑 이력 항목 변환 테스트")
class TrailingStopHistoryItemTest {

    @DisplayName("추적 전이면 등록 시점 기준가·발동가로 변환한다")
    @Test
    void givenNotTrailed_whenConverting_thenRegisteredPrices() {
        TrailingStopEntity entity = entity();

        TrailingStopHistoryItem item = TrailingStopHistoryItem.fromEntity(entity);

        assertThat(item.trailingStopId()).isEqualTo(1L);
        assertThat(item.stockCode()).isEqualTo("005930");
        assertThat(item.trailingStopType()).isEqualTo(TrailingStopType.SELL);
        assertThat(item.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(item.orderQuantity()).isEqualTo(10);
        assertThat(item.stopPercent()).isEqualTo(3.0);
        assertThat(item.basePrice()).isEqualTo(70_000);
        assertThat(item.triggerPrice()).isEqualTo(67_900);
        assertThat(item.trailingStopStatus()).isEqualTo(TrailingStopStatus.ACTIVE);
        assertThat(item.orderTime()).isEqualTo(entity.getOrderTime());
    }

    @DisplayName("추적 중이면 저장된 현재 기준가·발동가로 변환한다")
    @Test
    void givenTrailed_whenConverting_thenCurrentPrices() {
        TrailingStopEntity entity = entity();
        entity.setCurrentBasePrice(72_000);
        entity.setCurrentTriggerPrice(69_800);

        TrailingStopHistoryItem item = TrailingStopHistoryItem.fromEntity(entity);

        assertThat(item.basePrice()).isEqualTo(72_000);
        assertThat(item.triggerPrice()).isEqualTo(69_800);
    }

    private TrailingStopEntity entity() {
        TrailingStopEntity entity = TrailingStopEntity.of("user", "005930", TrailingStopType.SELL,
                LeverageRatio.X2, 10, 3.0, 70_000, 67_900, TrailingStopStatus.ACTIVE);
        entity.setTrailingStopId(1L);
        return entity;
    }
}
