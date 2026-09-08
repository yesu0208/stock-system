package arile.toy.stocksystem.stockserver.history.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record HistoryPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
    public static <T> HistoryPageResponse<T> of(Page<T> page) {
        return new HistoryPageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.hasNext());
    }
}
