package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 트레일링 스탑 DTO 변환 테스트")
class TrailingStopDtoTest {

    @DisplayName("저장된 추적값이 없으면 등록 시점 기준가·발동가로 추적을 시작하고, 초기 발동가는 등록 시점 발동가다")
    @Test
    void givenNoPersistedTrail_whenConverting_thenUsesRegisteredValues() {
        TrailingStopEntity entity = entity();

        TrailingStopDto dto = TrailingStopDto.fromEntity(entity);

        assertThat(dto.trailingStopId()).isEqualTo(1L);
        assertThat(dto.basePrice()).isEqualTo(70_000);
        assertThat(dto.triggerPrice()).isEqualTo(67_900);
        assertThat(dto.initialTriggerPrice()).isEqualTo(67_900);
        assertThat(dto.stopPercent()).isEqualTo(3.0);
        assertThat(dto.orderTime()).isEqualTo(entity.getOrderTime());
    }

    @DisplayName("저장된 추적값이 있으면 그 값으로 추적을 이어가고, 초기 발동가는 등록 시점 값을 유지한다")
    @Test
    void givenPersistedTrail_whenConverting_thenResumesTrail() {
        TrailingStopEntity entity = entity();
        entity.setCurrentBasePrice(72_000);
        entity.setCurrentTriggerPrice(69_800);

        TrailingStopDto dto = TrailingStopDto.fromEntity(entity);

        assertThat(dto.basePrice()).isEqualTo(72_000);
        assertThat(dto.triggerPrice()).isEqualTo(69_800);
        assertThat(dto.initialTriggerPrice()).isEqualTo(67_900);
    }

    @DisplayName("추적 갱신은 기준가·발동가만 바꾸고 나머지(초기 발동가 포함)는 유지한다")
    @Test
    void givenDto_whenUpdatingTrail_thenOnlyTrailChanges() {
        TrailingStopDto dto = TrailingStopDto.fromEntity(entity());

        TrailingStopDto updated = dto.withUpdatedTrail(72_000, 69_800);

        assertThat(updated.basePrice()).isEqualTo(72_000);
        assertThat(updated.triggerPrice()).isEqualTo(69_800);
        assertThat(updated)
                .usingRecursiveComparison()
                .ignoringFields("basePrice", "triggerPrice")
                .isEqualTo(dto);
    }

    private TrailingStopEntity entity() {
        TrailingStopEntity entity = TrailingStopEntity.of("user", "005930", TrailingStopType.SELL,
                LeverageRatio.SPOT, 10, 3.0, 70_000, 67_900, TrailingStopStatus.ACTIVE);
        entity.setTrailingStopId(1L);
        return entity;
    }
}
