package arile.toy.stocksystem.bffserver.news.dto;

import java.util.List;

public record NaverNewsResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverNewsItem> items
) {
}