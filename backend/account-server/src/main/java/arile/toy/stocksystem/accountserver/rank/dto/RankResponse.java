package arile.toy.stocksystem.accountserver.rank.dto;

import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;

public record RankResponse(
        String username,
        String tier,
        Integer subTier,
        Long rp,
        String highestTierReached,
        Long currentRankMinRp,
        Long nextRankMinRp
) {
    public static RankResponse fromEntity(UserRankEntity entity) {
        RankLevel currentLevel = entity.getCurrentLevel();
        RankLevel nextLevel = resolveNextLevel(currentLevel);

        return new RankResponse(
                entity.getUsername(),
                currentLevel.getTier().name(),
                currentLevel.getSubTier(),
                entity.getRp(),
                entity.getHighestTierReached().name(),
                currentLevel.getRpLower(),
                nextLevel != null ? nextLevel.getRpLower() : null
        );
    }

    /** enum 선언 순서상 "한 단계 위" 등급을 찾음. 최고 등급(MASTER)이면 null. */
    private static RankLevel resolveNextLevel(RankLevel current) {
        RankLevel[] levels = RankLevel.values();
        int idx = current.ordinal();
        return idx + 1 < levels.length ? levels[idx + 1] : null;
    }
}
