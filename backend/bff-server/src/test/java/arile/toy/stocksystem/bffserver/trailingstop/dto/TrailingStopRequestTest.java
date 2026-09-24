package arile.toy.stocksystem.bffserver.trailingstop.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrailingStopRequestTest {

    @Test
    @DisplayName("레버리지 배율이 없으면 SPOT으로, 있으면 그대로 사용한다")
    void leverageRatioOrDefault() {
        assertThat(new TrailingStopRequest("005930", TrailingStopType.SELL, 10, 3.0, 70_000, null)
                .leverageRatioOrDefault()).isEqualTo(LeverageRatio.SPOT);
        assertThat(new TrailingStopRequest("005930", TrailingStopType.SELL, 10, 3.0, 70_000, LeverageRatio.X2)
                .leverageRatioOrDefault()).isEqualTo(LeverageRatio.X2);
    }
}
