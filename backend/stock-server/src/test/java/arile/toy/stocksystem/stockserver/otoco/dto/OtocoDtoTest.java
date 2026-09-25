package arile.toy.stocksystem.stockserver.otoco.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] OTOCO 변환 테스트")
class OtocoDtoTest {

    @DisplayName("엔티티를 DTO·응답 메시지·이력 항목으로 변환한다")
    @Test
    void whenConverting_thenMapsFields() {
        OtocoEntity entity = OtocoFixtures.entity(1L, OtocoStatus.ENTRY_ORDER_PLACED, LeverageRatio.X2);
        entity.setEntryOrderId(100L);

        OtocoDto dto = OtocoDto.fromEntity(entity, 4);
        assertThat(dto.otocoId()).isEqualTo(1L);
        assertThat(dto.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(dto.entryTriggerPrice()).isEqualTo(70_000);
        assertThat(dto.tpTriggerPrice()).isEqualTo(73_500);
        assertThat(dto.slTriggerPrice()).isEqualTo(67_900);
        assertThat(dto.entryOrderId()).isEqualTo(100L);
        assertThat(dto.entryRemainingQuantity()).isEqualTo(4);
        assertThat(OtocoDto.fromEntity(entity).entryRemainingQuantity()).isNull();

        var message = StockServerOtocoResponseMessage.fromEntity(entity, 4);
        assertThat(message.otocoStatus()).isEqualTo(OtocoStatus.ENTRY_ORDER_PLACED);
        assertThat(message.entryRemainingQuantity()).isEqualTo(4);
        assertThat(StockServerOtocoResponseMessage.fromEntity(entity).entryRemainingQuantity()).isNull();

        entity.markCompleted(OtocoLeg.STOP_LOSS);
        var item = OtocoHistoryItem.fromEntity(entity);
        assertThat(item.otocoStatus()).isEqualTo(OtocoStatus.COMPLETED);
        assertThat(item.completedLeg()).isEqualTo(OtocoLeg.STOP_LOSS);
    }

    @DisplayName("신규 엔티티는 WAITING_ENTRY로 생성된다")
    @Test
    void whenCreating_thenWaitingEntry() {
        OtocoEntity entity = OtocoEntity.of("user", "005930", OtocoEntryDirection.ABOVE, LeverageRatio.SPOT, 10, 70_000,
                OtocoExitMode.PCT, null, 5.0, 73_500, OtocoExitMode.PCT, null, 3.0, 67_900);

        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.WAITING_ENTRY);
        assertThat(entity.getCompletedLeg()).isNull();
        assertThat(entity.getOrderTime()).isNotNull();
    }
}
