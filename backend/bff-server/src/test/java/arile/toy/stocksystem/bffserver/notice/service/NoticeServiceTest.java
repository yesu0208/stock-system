package arile.toy.stocksystem.bffserver.notice.service;

import arile.toy.stocksystem.bffserver.exception.notice.NoticeNotFoundException;
import arile.toy.stocksystem.bffserver.notice.dto.CursorPage;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeCreateRequest;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeDetail;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeEditRequest;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeSummary;
import arile.toy.stocksystem.bffserver.notice.entity.NoticeEntity;
import arile.toy.stocksystem.bffserver.notice.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeService service;

    private static NoticeEntity notice(Long noticeId) {
        NoticeEntity notice = NoticeEntity.of("제목", "내용", "admin");
        notice.setNoticeId(noticeId);
        return notice;
    }

    @Nested
    @DisplayName("작성 · 조회 · 수정 · 삭제")
    class Crud {

        @Test
        @DisplayName("작성: 요청자를 작성자로 저장하고 상세를 반환한다")
        void create() {
            given(noticeRepository.save(any(NoticeEntity.class))).willAnswer(invocation -> {
                NoticeEntity saved = invocation.getArgument(0);
                saved.setNoticeId(1L);
                return saved;
            });

            NoticeDetail detail = service.createNotice("admin", new NoticeCreateRequest("점검 안내", "내일 점검합니다."));

            assertThat(detail.noticeId()).isEqualTo(1L);
            assertThat(detail.title()).isEqualTo("점검 안내");
            assertThat(detail.authorId()).isEqualTo("admin");
        }

        @Test
        @DisplayName("조회: 공지 상세를 반환하고, 없으면 NoticeNotFoundException")
        void get() {
            given(noticeRepository.findById(1L)).willReturn(Optional.of(notice(1L)));
            given(noticeRepository.findById(2L)).willReturn(Optional.empty());

            assertThat(service.getNotice(1L).noticeId()).isEqualTo(1L);
            assertThatThrownBy(() -> service.getNotice(2L)).isInstanceOf(NoticeNotFoundException.class);
        }

        @Test
        @DisplayName("수정: 제목과 내용을 바꾼다")
        void edit() {
            NoticeEntity notice = notice(1L);
            given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

            NoticeDetail detail = service.editNotice(1L, new NoticeEditRequest("새 제목", "새 내용"));

            assertThat(notice.getTitle()).isEqualTo("새 제목");
            assertThat(detail.content()).isEqualTo("새 내용");
        }

        @Test
        @DisplayName("수정: 없는 공지면 NoticeNotFoundException")
        void edit_notFound() {
            given(noticeRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.editNotice(1L, new NoticeEditRequest("t", "c")))
                    .isInstanceOf(NoticeNotFoundException.class);
        }

        @Test
        @DisplayName("삭제: 공지를 삭제하고, 없는 공지면 NoticeNotFoundException이며 아무것도 지우지 않는다")
        void delete() {
            NoticeEntity notice = notice(1L);
            given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));
            given(noticeRepository.findById(2L)).willReturn(Optional.empty());

            service.deleteNotice(1L);
            verify(noticeRepository).delete(notice);

            assertThatThrownBy(() -> service.deleteNotice(2L)).isInstanceOf(NoticeNotFoundException.class);
            verify(noticeRepository, never()).delete(org.mockito.ArgumentMatchers.argThat(n -> n.getNoticeId() == 2L));
        }
    }

    @Nested
    @DisplayName("목록")
    class Paging {

        private List<NoticeEntity> notices(int count) {
            return LongStream.rangeClosed(1, count).map(i -> 100 - i).mapToObj(NoticeServiceTest::notice).toList();
        }

        @Test
        @DisplayName("20개보다 1개 더 조회되면 20개만 담고, 마지막 공지 ID를 다음 커서로 준다")
        void hasNext() {
            given(noticeRepository.findAllByCursor(eq(null), any())).willReturn(notices(21));

            CursorPage<NoticeSummary> page = service.getNotices(null);

            assertThat(page.items()).hasSize(20);
            assertThat(page.hasNext()).isTrue();
            assertThat(page.nextCursor()).isEqualTo(80L);
        }

        @Test
        @DisplayName("20개 이하면 다음 페이지가 없다")
        void lastPage() {
            given(noticeRepository.findAllByCursor(eq(80L), any())).willReturn(notices(3));

            CursorPage<NoticeSummary> page = service.getNotices(80L);

            assertThat(page.items()).hasSize(3);
            assertThat(page.hasNext()).isFalse();
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        @DisplayName("공지가 없으면 빈 페이지를 반환한다")
        void empty() {
            given(noticeRepository.findAllByCursor(eq(null), any())).willReturn(List.of());

            CursorPage<NoticeSummary> page = service.getNotices(null);

            assertThat(page.items()).isEmpty();
            assertThat(page.hasNext()).isFalse();
            assertThat(page.nextCursor()).isNull();
        }
    }
}
