package arile.toy.stocksystem.bffserver.autoorder.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AutoOrderResponseMessageTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("stock-server가 Redis에 저장한 레버리지 자동 주문(증거금·청산가 포함)을 필드 그대로 읽는다")
    void deserialize_leverage() throws Exception {
        AutoOrderResponseMessage message = OBJECT_MAPPER.readValue("""
                {"autoOrderId": 7, "username": "user1", "stockCode": "005930", "autoOrderType": "BUY",
                 "leverageRatio": "X2", "triggerPrice": 68000, "orderPrice": 69000, "orderQuantity": 10,
                 "orderTime": "2026-09-24T00:30:00Z", "notionalValue": 690000, "initialMargin": 345000,
                 "maintenanceMarginRate": 1.4, "liquidationPrice": 48300}
                """, AutoOrderResponseMessage.class);

        assertThat(message).isEqualTo(new AutoOrderResponseMessage(
                7L, "user1", "005930", AutoOrderType.BUY, LeverageRatio.X2, 68_000, 69_000, 10,
                Instant.parse("2026-09-24T00:30:00Z"), 690_000L, 345_000L, 1.4, 48_300L));
    }

    @Test
    @DisplayName("현금 주문은 증거금·유지증거금률·청산가가 없다(null)")
    void deserialize_spot() throws Exception {
        AutoOrderResponseMessage message = OBJECT_MAPPER.readValue("""
                {"autoOrderId": 8, "username": "user1", "stockCode": "005930", "autoOrderType": "SELL",
                 "leverageRatio": "SPOT", "triggerPrice": 72000, "orderPrice": 71500, "orderQuantity": 5,
                 "orderTime": "2026-09-24T00:30:00Z", "notionalValue": 357500}
                """, AutoOrderResponseMessage.class);

        assertThat(message.initialMargin()).isNull();
        assertThat(message.maintenanceMarginRate()).isNull();
        assertThat(message.liquidationPrice()).isNull();
        assertThat(message.notionalValue()).isEqualTo(357_500L);
    }
}
