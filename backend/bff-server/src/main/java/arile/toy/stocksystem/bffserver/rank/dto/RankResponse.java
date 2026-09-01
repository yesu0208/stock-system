package arile.toy.stocksystem.bffserver.rank.dto;

public record RankResponse(
        String username,
        String tier,
        Integer subTier,
        Long rp,
        String highestTierReached
) {
    public RankTier currentRankTier() {
        try {
            return RankTier.valueOf(this.tier);
        } catch (IllegalArgumentException | NullPointerException e) {
            return RankTier.UNRANKED;
        }
    }

    public boolean isAtLeast(RankTier requiredTier) {
        return currentRankTier().ordinal() >= requiredTier.ordinal();
    }
}
