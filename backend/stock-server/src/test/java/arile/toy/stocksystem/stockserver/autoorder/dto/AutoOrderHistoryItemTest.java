package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 자동주문 내역 항목 변환 테스트")
class AutoOrderHistoryItemTest {

    @DisplayName("엔티티 필드를 옮겨 담고, 주문가·수량 기준 레버리지 지표를 채운다")
    @Test
    void givenLeverageAutoOrder_whenConverting_thenCopiesFieldsAndMetrics() {
        // 주문가 70,000 × 10주, X2 → 명목 700,000 / 증거금 350,000 / 청산가 49,000
        AutoOrderEntity entity = AutoOrderEntity.of("user", "005930", AutoOrderType.BUY, LeverageRatio.X2,
                71_000, 70_000, 10, AutoOrderStatus.TRIGGERED);
        entity.setAutoOrderId(1L);

        AutoOrderHistoryItem item = AutoOrderHistoryItem.fromEntity(entity);

        assertThat(item.autoOrderId()).isEqualTo(1L);
        assertThat(item.stockCode()).isEqualTo("005930");
        assertThat(item.autoOrderType()).isEqualTo(AutoOrderType.BUY);
        assertThat(item.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(item.triggerPrice()).isEqualTo(71_000);
        assertThat(item.orderPrice()).isEqualTo(70_000);
        assertThat(item.orderQuantity()).isEqualTo(10);
        assertThat(item.autoOrderStatus()).isEqualTo(AutoOrderStatus.TRIGGERED);
        assertThat(item.orderTime()).isEqualTo(entity.getOrderTime());

        assertThat(item.notionalValue()).isEqualTo(700_000L);
        assertThat(item.initialMargin()).isEqualTo(350_000L);
        assertThat(item.maintenanceMarginRate()).isEqualTo(1.4);
        assertThat(item.liquidationPrice()).isEqualTo(49_000L);
    }

    @DisplayName("현물 자동주문은 명목금액만 채우고 레버리지 지표는 null이다")
    @Test
    void givenSpotAutoOrder_whenConverting_thenNoLeverageMetrics() {
        AutoOrderEntity entity = AutoOrderEntity.of("user", "005930", AutoOrderType.SELL, LeverageRatio.SPOT,
                69_000, 70_000, 10, AutoOrderStatus.ACTIVE);

        AutoOrderHistoryItem item = AutoOrderHistoryItem.fromEntity(entity);

        assertThat(item.notionalValue()).isEqualTo(700_000L);
        assertThat(item.initialMargin()).isNull();
        assertThat(item.maintenanceMarginRate()).isNull();
        assertThat(item.liquidationPrice()).isNull();
    }
}
