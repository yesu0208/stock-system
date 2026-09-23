package arile.toy.stocksystem.bffserver.autoorder.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutoOrderRequestTest {

    @Test
    @DisplayName("레버리지 배율이 없으면 SPOT으로, 있으면 그대로 사용한다")
    void leverageRatioOrDefault() {
        assertThat(new AutoOrderRequest("005930", AutoOrderType.BUY, 68_000, 69_000, 10, null)
                .leverageRatioOrDefault()).isEqualTo(LeverageRatio.SPOT);
        assertThat(new AutoOrderRequest("005930", AutoOrderType.BUY, 68_000, 69_000, 10, LeverageRatio.X2)
                .leverageRatioOrDefault()).isEqualTo(LeverageRatio.X2);
    }
}
