package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionCommentEntity;

import java.time.Instant;

public record CommentResponse(
        Long commentId,
        String authorId,
        Instant createdDateTime,
        Instant updatedDateTime,
        String content,
        int likes,
        int dislikes
) {
    public static CommentResponse of(DiscussionCommentEntity entity, int likes, int dislikes) {
        return new CommentResponse(
                entity.getCommentId(),
                entity.getAuthorId(),
                entity.getCreatedDateTime(),
                entity.getUpdatedDateTime(),
                entity.getContent(),
                likes,
                dislikes
        );
    }
}
