package arile.toy.stocksystem.bffserver.leverage.dto;

import arile.toy.stocksystem.bffserver.rank.dto.RankTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeverageRatioTest {

    @Test
    @DisplayName("현금(SPOT)만 isSpot이 참이다")
    void isSpot() {
        assertThat(LeverageRatio.SPOT.isSpot()).isTrue();
        assertThat(LeverageRatio.X1_5.isSpot()).isFalse();
        assertThat(LeverageRatio.X2.isSpot()).isFalse();
        assertThat(LeverageRatio.X2_5.isSpot()).isFalse();
    }

    @Test
    @DisplayName("배율별 필요 등급: 1.5배까지는 제한 없음, 2배는 GOLD, 2.5배는 PLATINUM")
    void requiredTier() {
        assertThat(LeverageRatio.SPOT.requiredTier()).isNull();
        assertThat(LeverageRatio.X1_5.requiredTier()).isNull();
        assertThat(LeverageRatio.X2.requiredTier()).isEqualTo(RankTier.GOLD);
        assertThat(LeverageRatio.X2_5.requiredTier()).isEqualTo(RankTier.PLATINUM);
    }
}
