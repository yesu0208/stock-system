package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderResponseMessageTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private static final OrderExecutionType EXECUTION = OrderExecutionType.values()[0];
    private static final OrderOrigin ORIGIN = OrderOrigin.values()[0];

    @Test
    @DisplayName("stock-server가 Redis에 저장한 미체결 주문(증거금·청산가·주문 출처 포함)을 필드 그대로 읽는다")
    void deserialize() throws Exception {
        OrderResponseMessage message = OBJECT_MAPPER.readValue("""
                {"orderId": 11, "username": "user1", "stockCode": "005930", "orderType": "BUY",
                 "leverageRatio": "X2", "orderPrice": 69000, "orderQuantity": 10, "remainingQuantity": 4,
                 "orderTime": "2026-09-24T00:30:00Z", "notionalValue": 690000, "initialMargin": 345000,
                 "maintenanceMarginRate": 1.4, "liquidationPrice": 48300,
                 "orderExecutionType": "%s", "origin": "%s", "originId": 7}
                """.formatted(EXECUTION.name(), ORIGIN.name()), OrderResponseMessage.class);

        assertThat(message).isEqualTo(new OrderResponseMessage(
                11L, "user1", "005930", OrderType.BUY, LeverageRatio.X2, 69_000, 10, 4,
                Instant.parse("2026-09-24T00:30:00Z"), 690_000L, 345_000L, 1.4, 48_300L,
                EXECUTION, ORIGIN, 7L));
    }
}
