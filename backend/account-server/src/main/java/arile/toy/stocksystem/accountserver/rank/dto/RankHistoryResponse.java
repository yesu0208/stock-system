package arile.toy.stocksystem.accountserver.rank.dto;

import java.util.List;

public record RankHistoryResponse(
        List<RankHistoryItem> items,
        boolean hasNext
) {
}
