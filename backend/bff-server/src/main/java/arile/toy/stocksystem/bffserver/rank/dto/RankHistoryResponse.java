package arile.toy.stocksystem.bffserver.rank.dto;

import java.util.List;

public record RankHistoryResponse(
        List<RankHistoryItem> items,
        boolean hasNext
) {
}
