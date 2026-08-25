package arile.toy.stocksystem.bffserver.news.dto;

public record NaverNewsItem(
        String title,
        String originallink,
        String link,
        String description,
        String pubDate
) {
}