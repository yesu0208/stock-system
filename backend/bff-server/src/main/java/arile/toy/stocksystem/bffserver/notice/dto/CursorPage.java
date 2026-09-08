package arile.toy.stocksystem.bffserver.notice.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        Long nextCursor,
        boolean hasNext
) {
}
