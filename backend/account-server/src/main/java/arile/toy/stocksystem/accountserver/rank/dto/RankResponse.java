package arile.toy.stocksystem.accountserver.rank.dto;

public record RankResponse(
        String username,
        String tier,
        Integer subTier,
        Long rp,
        String highestTierReached
) {
    public static RankResponse fromEntity(arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity entity) {
        return new RankResponse(
                entity.getUsername(),
                entity.getCurrentLevel().getTier().name(),
                entity.getCurrentLevel().getSubTier(),
                entity.getRp(),
                entity.getHighestTierReached().name()
        );
    }
}
