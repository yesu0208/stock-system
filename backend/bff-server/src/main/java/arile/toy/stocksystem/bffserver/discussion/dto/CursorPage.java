package arile.toy.stocksystem.bffserver.discussion.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        Long nextCursor,
        boolean hasNext
) {
}
