package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionPostEntity;
import arile.toy.stocksystem.bffserver.user.dto.UserProfile;

import java.time.Instant;
import java.util.List;

public record PostDetail(
        Long postId,
        String stockCode,
        String stockName,
        String title,
        String authorId,
        String authorNickname,
        String authorProfileImageUrl,
        Instant createdDateTime,
        Instant updatedDateTime,
        String content,
        int likes,
        int dislikes,
        int scraps,
        List<CommentResponse> comments
) {
    public static PostDetail of(
            DiscussionPostEntity entity, UserProfile authorProfile, int likes, int dislikes,
            int scraps, List<CommentResponse> comments
    ) {
        return new PostDetail(
                entity.getPostId(),
                entity.getStockCode(),
                entity.getStockName(),
                entity.getTitle(),
                entity.getAuthorId(),
                authorProfile.nickname(),
                authorProfile.profileImageUrl(),
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
