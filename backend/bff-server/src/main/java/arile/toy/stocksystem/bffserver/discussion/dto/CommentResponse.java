package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionCommentEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.ReactionType;
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
        int dislikes,
        ReactionType myReaction
) {
    public static CommentResponse of(
            DiscussionCommentEntity comment, UserProfile authorProfile,
            int likes, int dislikes, ReactionType myReaction
    ) {
        return new CommentResponse(
                comment.getCommentId(),
                comment.getAuthorId(),
                authorProfile.nickname(),
                authorProfile.profileImageUrl(),
                comment.getCreatedDateTime(),
                comment.getUpdatedDateTime(),
                comment.getContent(),
                likes,
                dislikes,
                myReaction
        );
    }
}
