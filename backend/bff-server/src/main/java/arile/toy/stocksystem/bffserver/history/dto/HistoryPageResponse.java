package arile.toy.stocksystem.bffserver.history.dto;

import java.util.List;

public record HistoryPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
}
