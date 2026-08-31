package arile.toy.stocksystem.accountserver.rank.dto;

import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;

import java.time.LocalDate;

public record RankHistoryItem(
        LocalDate date,
        String tier,
        Integer subTier,
        Long rp,
        Long rpChange
) {
    public static RankHistoryItem fromEntity(RankHistoryEntity entity) {
        return new RankHistoryItem(
                entity.getRecordDate(),
                entity.getRankLevel().getTier().name(),
                entity.getRankLevel().getSubTier(),
                entity.getRp(),
                entity.getRpChange()
        );
    }
}
