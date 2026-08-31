package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionPostEntity;

import java.time.Instant;

public record PostSummary(
        Long postId,
        String stockCode,
        String stockName,
        String title,
        String authorId,
        Instant createdDateTime,
        String contentPreview,
        int likes,
        int dislikes,
        int commentCount,
        int scraps
) {
    private static final int PREVIEW_LENGTH = 100;

    public static PostSummary of(DiscussionPostEntity entity, int likes, int dislikes,
                                 int commentCount, int scraps) {
        return new PostSummary(
                entity.getPostId(),
                entity.getStockCode(),
                entity.getStockName(),
                entity.getTitle(),
                entity.getAuthorId(),
                entity.getCreatedDateTime(),
                preview(entity.getContent()),
                likes,
                dislikes,
                commentCount,
                scraps
        );
    }

    private static String preview(String content) {
        return content.length() <= PREVIEW_LENGTH
                ? content
                : content.substring(0, PREVIEW_LENGTH) + "…";
    }
}
