package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionCommentEntity;
import arile.toy.stocksystem.bffserver.user.dto.UserProfile;

import java.time.Instant;

public record CommentResponse(
        Long commentId,
        String authorId,
        String authorNickname,
        String authorProfileImageUrl,
        Instant createdDateTime,
        Instant updatedDateTime,
        String content,
        int likes,
        int dislikes
) {
    public static CommentResponse of(
            DiscussionCommentEntity entity, UserProfile authorProfile, int likes, int dislikes
    ) {
        return new CommentResponse(
                entity.getCommentId(),
                entity.getAuthorId(),
                authorProfile.nickname(),
                authorProfile.profileImageUrl(),
                entity.getCreatedDateTime(),
                entity.getUpdatedDateTime(),
                entity.getContent(),
                likes,
                dislikes
        );
    }
}
