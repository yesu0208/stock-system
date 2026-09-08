package arile.toy.stocksystem.bffserver.dailyreturn.dto;

import java.util.List;

public record DailyReturnHistoryResponse(
        List<DailyReturnHistoryItem> items,
        boolean hasNext
) {
}
