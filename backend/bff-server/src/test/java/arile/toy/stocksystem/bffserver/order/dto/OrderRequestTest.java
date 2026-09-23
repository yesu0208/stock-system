package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRequestTest {

    @Test
    @DisplayName("레버리지 배율과 체결 방식이 없으면 SPOT·LIMIT으로 간주한다 (기존 클라이언트 호환)")
    void defaults() {
        OrderRequest request = new OrderRequest("005930", OrderType.BUY, 70_000, 10, null, null);

        assertThat(request.leverageRatioOrDefault()).isEqualTo(LeverageRatio.SPOT);
        assertThat(request.orderExecutionTypeOrDefault()).isEqualTo(OrderExecutionType.LIMIT);
    }

    @Test
    @DisplayName("값이 있으면 그대로 사용한다")
    void explicitValues() {
        OrderRequest request = new OrderRequest(
                "005930", OrderType.SELL, 70_000, 10, LeverageRatio.X2, OrderExecutionType.MARKET);

        assertThat(request.leverageRatioOrDefault()).isEqualTo(LeverageRatio.X2);
        assertThat(request.orderExecutionTypeOrDefault()).isEqualTo(OrderExecutionType.MARKET);
    }
}
