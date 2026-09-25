package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoExitMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Util] OTOCO 익절·손절가 계산 테스트")
class OtocoPriceResolverTest {

    @DisplayName("PRICE 모드는 입력한 가격을 그대로 사용한다")
    @Test
    void givenPriceMode_whenResolving_thenReturnsPrice() {
        assertThat(OtocoPriceResolver.resolveTakeProfit(70_000, OtocoExitMode.PRICE, 75_000, null)).isEqualTo(75_000);
        assertThat(OtocoPriceResolver.resolveStopLoss(70_000, OtocoExitMode.PRICE, 65_000, null)).isEqualTo(65_000);
    }

    @DisplayName("PCT 모드: 익절가는 호가 단위로 올림, 손절가는 내림한다")
    @Test
    void givenPctMode_whenResolving_thenRoundsToTick() {
        assertThat(OtocoPriceResolver.resolveTakeProfit(70_000, OtocoExitMode.PCT, null, 5.0)).isEqualTo(73_500);
        assertThat(OtocoPriceResolver.resolveStopLoss(70_000, OtocoExitMode.PCT, null, 3.0)).isEqualTo(67_900);
        // 72,310 → 72,400 / 67,690 → 67,600 (호가 100원)
        assertThat(OtocoPriceResolver.resolveTakeProfit(70_000, OtocoExitMode.PCT, null, 3.3)).isEqualTo(72_400);
        assertThat(OtocoPriceResolver.resolveStopLoss(70_000, OtocoExitMode.PCT, null, 3.3)).isEqualTo(67_600);
    }
}
