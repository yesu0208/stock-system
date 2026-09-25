package arile.toy.stocksystem.bffserver.discussion.dto;

import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionPostEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.ReactionType;
import arile.toy.stocksystem.bffserver.user.dto.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PostSummaryTest {

    private static PostSummary summary(String content) {
        DiscussionPostEntity post = DiscussionPostEntity.of("005930", "삼성전자", "제목", "author", content);
        ReflectionTestUtils.setField(post, "postId", 1L);
        UserProfile profile = mock(UserProfile.class);
        given(profile.nickname()).willReturn("닉네임");
        given(profile.profileImageUrl()).willReturn("/uploads/profile/author.png");

        return PostSummary.of(post, profile, 3, 1, 5, 2, ReactionType.LIKE, true);
    }

    @Test
    @DisplayName("본문이 100자 이하면 미리보기에 그대로 담는다")
    void preview_withinLimit() {
        String exactly100 = "가".repeat(100);

        assertThat(summary(exactly100).contentPreview()).isEqualTo(exactly100);
    }

    @Test
    @DisplayName("본문이 100자를 넘으면 100자에서 자르고 말줄임표를 붙인다")
    void preview_truncated() {
        assertThat(summary("가".repeat(101)).contentPreview()).isEqualTo("가".repeat(100) + "…");
    }

    @Test
    @DisplayName("게시글·작성자 정보와 반응·댓글·스크랩 수, 내 반응·스크랩 여부를 그대로 담는다")
    void fields() {
        PostSummary summary = summary("내용");

        assertThat(summary.postId()).isEqualTo(1L);
        assertThat(summary.stockCode()).isEqualTo("005930");
        assertThat(summary.authorNickname()).isEqualTo("닉네임");
        assertThat(summary.authorProfileImageUrl()).isEqualTo("/uploads/profile/author.png");
        assertThat(summary.likes()).isEqualTo(3);
        assertThat(summary.dislikes()).isEqualTo(1);
        assertThat(summary.commentCount()).isEqualTo(5);
        assertThat(summary.scraps()).isEqualTo(2);
        assertThat(summary.myReaction()).isEqualTo(ReactionType.LIKE);
        assertThat(summary.myScrapped()).isTrue();
    }
}
