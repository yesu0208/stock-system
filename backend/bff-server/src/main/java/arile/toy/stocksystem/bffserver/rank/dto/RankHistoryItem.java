package arile.toy.stocksystem.bffserver.rank.dto;

import java.time.LocalDate;

public record RankHistoryItem(
        LocalDate date,
        String tier,
        Integer subTier,
        Long rp,
        Long rpChange
) {
}
