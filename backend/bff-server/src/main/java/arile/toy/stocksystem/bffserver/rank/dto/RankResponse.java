package arile.toy.stocksystem.bffserver.rank.dto;

public record RankResponse(
        String username,
        String tier,
        Integer subTier,
        Long rp,
        String highestTierReached
) {
}
