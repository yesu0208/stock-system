package arile.toy.stocksystem.accountserver.dailyreturn.dto;

import java.util.List;

public record DailyReturnHistoryResponse(
        List<DailyReturnHistoryItem> items,
        boolean hasNext
) {
}
