package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionPostEntity;

import java.time.Instant;
import java.util.List;

public record PostDetail(
        Long postId,
        String stockCode,
        String stockName,
        String title,
        String authorId,
        Instant createdDateTime,
        Instant updatedDateTime,
        String content,
        int likes,
        int dislikes,
        int scraps,
        List<CommentResponse> comments
) {
    public static PostDetail of(DiscussionPostEntity entity, int likes, int dislikes,
                                int scraps, List<CommentResponse> comments) {
        return new PostDetail(
                entity.getPostId(),
                entity.getStockCode(),
                entity.getStockName(),
                entity.getTitle(),
                entity.getAuthorId(),
                entity.getCreatedDateTime(),
                entity.getUpdatedDateTime(),
                entity.getContent(),
                likes,
                dislikes,
                scraps,
                comments
        );
    }
}
