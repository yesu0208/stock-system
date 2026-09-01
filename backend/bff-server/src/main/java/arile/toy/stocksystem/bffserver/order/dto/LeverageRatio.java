package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.rank.dto.RankTier;

public enum LeverageRatio {
    SPOT(null),
    X1_5(null),
    X2(RankTier.GOLD),
    X2_5(RankTier.PLATINUM);

    private final RankTier requiredTier;

    LeverageRatio(RankTier requiredTier) {
        this.requiredTier = requiredTier;
    }

    public boolean isSpot() {
        return this == SPOT;
    }

    /** null이면 등급 제한 없음 */
    public RankTier requiredTier() {
        return requiredTier;
    }
}
