package arile.toy.stocksystem.stockserver.trade.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[DTO] 체결 내역 항목 변환 테스트")
class TradeHistoryItemTest {

    @DisplayName("체결 엔티티의 필드를 옮겨 담고, 체결가·체결수량 기준 레버리지 지표를 채운다")
    @Test
    void givenLeverageTrade_whenConverting_thenCopiesFieldsAndMetrics() {
        // Given: 70,000원 × 10주, X2 → 명목 700,000 / 증거금 350,000 / 청산가 49,000
        TradeEntity trade = TradeEntity.of(1L, "user", "005930", TradeType.BUY, 70_000, 10,
                LeverageRatio.X2, OrderOrigin.OTOCO_ENTRY, 7L);
        trade.setTradeId(100L);

        // When
        TradeHistoryItem item = TradeHistoryItem.fromEntity(trade);

        // Then
        assertThat(item.tradeId()).isEqualTo(100L);
        assertThat(item.orderId()).isEqualTo(1L);
        assertThat(item.stockCode()).isEqualTo("005930");
        assertThat(item.tradeType()).isEqualTo(TradeType.BUY);
        assertThat(item.tradePrice()).isEqualTo(70_000);
        assertThat(item.tradeQuantity()).isEqualTo(10);
        assertThat(item.leverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(item.origin()).isEqualTo(OrderOrigin.OTOCO_ENTRY);
        assertThat(item.originId()).isEqualTo(7L);

        assertThat(item.notionalValue()).isEqualTo(700_000L);
        assertThat(item.initialMargin()).isEqualTo(350_000L);
        assertThat(item.maintenanceMarginRate()).isEqualTo(1.4);
        assertThat(item.liquidationPrice()).isEqualTo(49_000L);
    }

    @DisplayName("현물 체결은 명목금액만 채우고 레버리지 지표는 null이다")
    @Test
    void givenSpotTrade_whenConverting_thenNoLeverageMetrics() {
        TradeEntity trade = TradeEntity.of(1L, "user", "005930", TradeType.SELL, 70_000, 10,
                LeverageRatio.SPOT, OrderOrigin.MANUAL, null);

        TradeHistoryItem item = TradeHistoryItem.fromEntity(trade);

        assertThat(item.notionalValue()).isEqualTo(700_000L);
        assertThat(item.initialMargin()).isNull();
        assertThat(item.maintenanceMarginRate()).isNull();
        assertThat(item.liquidationPrice()).isNull();
    }
}
