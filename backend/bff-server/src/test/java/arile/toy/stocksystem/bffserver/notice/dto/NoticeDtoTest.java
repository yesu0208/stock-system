package arile.toy.stocksystem.bffserver.notice.dto;

import arile.toy.stocksystem.bffserver.notice.entity.NoticeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeDtoTest {

    private static NoticeEntity notice(String content) {
        NoticeEntity notice = NoticeEntity.of("제목", content, "admin");
        notice.setNoticeId(1L);
        return notice;
    }

    @Test
    @DisplayName("목록 미리보기: 100자 이하면 그대로, 넘으면 100자에서 자르고 말줄임표를 붙인다")
    void preview() {
        String exactly100 = "가".repeat(100);
        String over100 = "가".repeat(101);

        assertThat(NoticeSummary.of(notice(exactly100)).contentPreview()).isEqualTo(exactly100);
        assertThat(NoticeSummary.of(notice(over100)).contentPreview()).isEqualTo("가".repeat(100) + "…");
    }

    @Test
    @DisplayName("상세: 엔티티의 모든 필드를 그대로 옮긴다")
    void detail() {
        NoticeEntity notice = notice("내용");
        ReflectionTestUtils.invokeMethod(notice, "prePersist");

        NoticeDetail detail = NoticeDetail.of(notice);

        assertThat(detail.noticeId()).isEqualTo(1L);
        assertThat(detail.title()).isEqualTo("제목");
        assertThat(detail.content()).isEqualTo("내용");
        assertThat(detail.authorId()).isEqualTo("admin");
        assertThat(detail.createdDateTime()).isNotNull();
        assertThat(detail.updatedDateTime()).isNotNull();
    }

    @Test
    @DisplayName("엔티티: 수정하면 제목·내용이 바뀌고, 저장 전 생성·수정 시각을 채운다")
    void entity() {
        NoticeEntity notice = notice("내용");

        notice.edit("새 제목", "새 내용");
        ReflectionTestUtils.invokeMethod(notice, "prePersist");

        assertThat(notice.getTitle()).isEqualTo("새 제목");
        assertThat(notice.getContent()).isEqualTo("새 내용");
        assertThat(notice.getCreatedDateTime()).isNotNull();
    }
}
