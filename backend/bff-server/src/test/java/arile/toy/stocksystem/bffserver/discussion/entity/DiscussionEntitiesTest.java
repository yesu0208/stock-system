package arile.toy.stocksystem.bffserver.discussion.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class DiscussionEntitiesTest {

    @Test
    @DisplayName("게시글: 저장 전 생성 시각을 채운다")
    void post_prePersist() {
        DiscussionPostEntity post = DiscussionPostEntity.of("005930", "삼성전자", "제목", "author", "내용");

        ReflectionTestUtils.invokeMethod(post, "prePersist");

        assertThat(post.getCreatedDateTime()).isNotNull();
    }

    @Test
    @DisplayName("댓글: 저장 전 생성 시각을 채운다")
    void comment_prePersist() {
        DiscussionCommentEntity comment = DiscussionCommentEntity.of(1L, "author", "댓글");

        ReflectionTestUtils.invokeMethod(comment, "prePersist");

        assertThat(comment.getCreatedDateTime()).isNotNull();
    }
}
