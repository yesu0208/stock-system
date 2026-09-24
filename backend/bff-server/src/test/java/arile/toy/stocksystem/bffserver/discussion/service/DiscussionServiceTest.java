package arile.toy.stocksystem.bffserver.discussion.service;

import arile.toy.stocksystem.bffserver.discussion.dto.CommentCreateRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.CommentEditRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.CommentResponse;
import arile.toy.stocksystem.bffserver.discussion.dto.CursorPage;
import arile.toy.stocksystem.bffserver.discussion.dto.PostCreateRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.PostDetail;
import arile.toy.stocksystem.bffserver.discussion.dto.PostEditRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.PostSummary;
import arile.toy.stocksystem.bffserver.discussion.dto.ReactionRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.ReactionResponse;
import arile.toy.stocksystem.bffserver.discussion.dto.ScrapResponse;
import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionCommentEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionPostEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionReactionEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.DiscussionScrapEntity;
import arile.toy.stocksystem.bffserver.discussion.entity.ReactionType;
import arile.toy.stocksystem.bffserver.discussion.entity.TargetType;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionCommentRepository;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionPostRepository;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionReactionRepository;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionScrapRepository;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionCommentNotFoundException;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionForbiddenException;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionPostNotFoundException;
import arile.toy.stocksystem.bffserver.user.dto.UserProfile;
import arile.toy.stocksystem.bffserver.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiscussionServiceTest {

    private static final String AUTHOR = "author";
    private static final String OTHER = "other";

    @Mock private DiscussionPostRepository postRepository;
    @Mock private UserProfileService userProfileService;
    @Mock private DiscussionCommentRepository commentRepository;
    @Mock private DiscussionReactionRepository reactionRepository;
    @Mock private DiscussionScrapRepository scrapRepository;

    @InjectMocks
    private DiscussionService service;

    @BeforeEach
    void setUp() {
        given(userProfileService.getProfile(anyString())).willReturn(mock(UserProfile.class));
    }

    private static DiscussionPostEntity post(Long postId, String authorId) {
        DiscussionPostEntity post = DiscussionPostEntity.of("005930", "삼성전자", "제목", authorId, "내용");
        ReflectionTestUtils.setField(post, "postId", postId);
        return post;
    }

    private static DiscussionCommentEntity comment(Long commentId, Long postId, String authorId) {
        DiscussionCommentEntity comment = DiscussionCommentEntity.of(postId, authorId, "댓글");
        ReflectionTestUtils.setField(comment, "commentId", commentId);
        return comment;
    }

    private DiscussionPostEntity givenPost(Long postId, String authorId) {
        DiscussionPostEntity post = post(postId, authorId);
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        return post;
    }

    private DiscussionCommentEntity givenComment(Long commentId, Long postId, String authorId) {
        DiscussionCommentEntity comment = comment(commentId, postId, authorId);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        return comment;
    }

    // ===================== 게시글 =====================

    @Nested
    @DisplayName("게시글")
    class Post {

        @Test
        @DisplayName("작성: 요청자를 작성자로 저장한다")
        void create() {
            given(postRepository.save(any(DiscussionPostEntity.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.createPost(AUTHOR, new PostCreateRequest("005930", "삼성전자", "제목", "내용"));

            verify(postRepository).save(argThat(p -> p.getAuthorId().equals(AUTHOR)));
        }

        @Test
        @DisplayName("조회: 없는 게시글이면 DiscussionPostNotFoundException")
        void get_notFound() {
            given(postRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPost(1L, AUTHOR)).isInstanceOf(DiscussionPostNotFoundException.class);
        }

        @Test
        @DisplayName("상세: 댓글이 여러 개여도 반응 수·내 반응·작성자 정보를 한 번씩만 묶어서 조회한다")
        void detail_batchesCommentQueries() {
            givenPost(1L, AUTHOR);
            given(commentRepository.findByPostIdOrderByCommentIdAsc(1L)).willReturn(List.of(
                    comment(10L, 1L, AUTHOR), comment(11L, 1L, OTHER), comment(12L, 1L, OTHER)));

            PostDetail detail = service.getPost(1L, AUTHOR);

            assertThat(detail.comments()).hasSize(3);
            verify(reactionRepository, times(1)).countGroupByTargetIds(TargetType.COMMENT, List.of(10L, 11L, 12L));
            verify(reactionRepository, times(1))
                    .findByTargetTypeAndTargetIdInAndUserId(TargetType.COMMENT, List.of(10L, 11L, 12L), AUTHOR);
            verify(userProfileService, times(1)).getProfiles(any());
            verify(reactionRepository, never())
                    .countByTargetTypeAndTargetIdAndReactionType(eq(TargetType.COMMENT), anyLong(), any());
        }

        @Test
        @DisplayName("상세: 로그인하지 않은 사용자는 내 반응·내 스크랩을 조회하지 않는다")
        void detail_anonymous() {
            givenPost(1L, AUTHOR);
            given(commentRepository.findByPostIdOrderByCommentIdAsc(1L)).willReturn(List.of(comment(10L, 1L, AUTHOR)));

            PostDetail detail = service.getPost(1L, null);

            assertThat(detail.myReaction()).isNull();
            assertThat(detail.myScrapped()).isFalse();
            verify(reactionRepository, never()).findByTargetTypeAndTargetIdInAndUserId(any(), any(), any());
            verify(scrapRepository, never()).findByPostIdAndUserId(anyLong(), anyString());
        }

        @Test
        @DisplayName("수정: 작성자는 제목과 내용을 바꿀 수 있다")
        void edit_byAuthor() {
            DiscussionPostEntity post = givenPost(1L, AUTHOR);

            service.editPost(AUTHOR, 1L, new PostEditRequest("새 제목", "새 내용"));

            assertThat(post.getTitle()).isEqualTo("새 제목");
            assertThat(post.getContent()).isEqualTo("새 내용");
        }

        @Test
        @DisplayName("수정: 작성자가 아니면 DiscussionForbiddenException이고 내용은 바뀌지 않는다")
        void edit_byOther_forbidden() {
            DiscussionPostEntity post = givenPost(1L, AUTHOR);

            assertThatThrownBy(() -> service.editPost(OTHER, 1L, new PostEditRequest("탈취", "탈취")))
                    .isInstanceOf(DiscussionForbiddenException.class);

            assertThat(post.getTitle()).isEqualTo("제목");
        }

        @Test
        @DisplayName("삭제: 작성자가 지우면 댓글 반응 → 댓글 → 게시글 반응 → 스크랩 → 게시글 순으로 모두 정리한다")
        void delete_byAuthor_cascades() {
            DiscussionPostEntity post = givenPost(1L, AUTHOR);
            given(commentRepository.findByPostIdOrderByCommentIdAsc(1L))
                    .willReturn(List.of(comment(10L, 1L, OTHER), comment(11L, 1L, AUTHOR)));

            service.deletePost(AUTHOR, 1L);

            InOrder inOrder = inOrder(reactionRepository, commentRepository, scrapRepository, postRepository);
            inOrder.verify(reactionRepository).deleteByTargetTypeAndTargetIdIn(TargetType.COMMENT, List.of(10L, 11L));
            inOrder.verify(commentRepository).deleteByPostId(1L);
            inOrder.verify(reactionRepository).deleteByTargetTypeAndTargetIdIn(TargetType.POST, List.of(1L));
            inOrder.verify(scrapRepository).deleteByPostId(1L);
            inOrder.verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("삭제: 댓글이 없으면 댓글 반응 삭제는 건너뛴다")
        void delete_withoutComments() {
            givenPost(1L, AUTHOR);
            given(commentRepository.findByPostIdOrderByCommentIdAsc(1L)).willReturn(List.of());

            service.deletePost(AUTHOR, 1L);

            verify(reactionRepository, never()).deleteByTargetTypeAndTargetIdIn(eq(TargetType.COMMENT), any());
            verify(reactionRepository).deleteByTargetTypeAndTargetIdIn(TargetType.POST, List.of(1L));
        }

        @Test
        @DisplayName("삭제: 작성자가 아니면 DiscussionForbiddenException이고 아무것도 지우지 않는다")
        void delete_byOther_forbidden() {
            givenPost(1L, AUTHOR);

            assertThatThrownBy(() -> service.deletePost(OTHER, 1L)).isInstanceOf(DiscussionForbiddenException.class);

            verify(postRepository, never()).delete(any());
            verify(commentRepository, never()).deleteByPostId(anyLong());
        }
    }

    // ===================== 댓글 =====================

    @Nested
    @DisplayName("댓글")
    class Comment {

        @Test
        @DisplayName("작성: 게시글이 있으면 요청자를 작성자로 저장한다")
        void add() {
            givenPost(1L, AUTHOR);
            given(commentRepository.save(any(DiscussionCommentEntity.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.addComment(OTHER, 1L, new CommentCreateRequest("댓글"));

            verify(commentRepository).save(argThat(c -> c.getAuthorId().equals(OTHER) && c.getPostId().equals(1L)));
        }

        @Test
        @DisplayName("작성: 없는 게시글이면 DiscussionPostNotFoundException이고 저장하지 않는다")
        void add_postNotFound() {
            given(postRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.addComment(AUTHOR, 1L, new CommentCreateRequest("댓글")))
                    .isInstanceOf(DiscussionPostNotFoundException.class);

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("수정: 작성자는 내용을 바꿀 수 있다")
        void edit_byAuthor() {
            DiscussionCommentEntity comment = givenComment(10L, 1L, AUTHOR);

            service.editComment(AUTHOR, 1L, 10L, new CommentEditRequest("수정된 댓글"));

            assertThat(comment.getContent()).isEqualTo("수정된 댓글");
        }

        @Test
        @DisplayName("수정: 작성자가 아니면 DiscussionForbiddenException이고 내용은 바뀌지 않는다")
        void edit_byOther_forbidden() {
            DiscussionCommentEntity comment = givenComment(10L, 1L, AUTHOR);

            assertThatThrownBy(() -> service.editComment(OTHER, 1L, 10L, new CommentEditRequest("탈취")))
                    .isInstanceOf(DiscussionForbiddenException.class);

            assertThat(comment.getContent()).isEqualTo("댓글");
        }

        @Test
        @DisplayName("URL의 게시글에 속하지 않은 댓글이면 없는 댓글로 처리한다 (다른 게시글 댓글 조작 방지)")
        void commentOfOtherPost_notFound() {
            givenComment(10L, 2L, AUTHOR);

            assertThatThrownBy(() -> service.editComment(AUTHOR, 1L, 10L, new CommentEditRequest("x")))
                    .isInstanceOf(DiscussionCommentNotFoundException.class);
            assertThatThrownBy(() -> service.deleteComment(AUTHOR, 1L, 10L))
                    .isInstanceOf(DiscussionCommentNotFoundException.class);
        }

        @Test
        @DisplayName("없는 댓글이면 DiscussionCommentNotFoundException")
        void commentNotFound() {
            given(commentRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteComment(AUTHOR, 1L, 10L))
                    .isInstanceOf(DiscussionCommentNotFoundException.class);
        }

        @Test
        @DisplayName("삭제: 작성자가 지우면 모든 사용자의 반응을 함께 지운 뒤 댓글을 삭제한다")
        void delete_byAuthor_removesAllReactions() {
            DiscussionCommentEntity comment = givenComment(10L, 1L, AUTHOR);

            service.deleteComment(AUTHOR, 1L, 10L);

            InOrder inOrder = inOrder(reactionRepository, commentRepository);
            inOrder.verify(reactionRepository).deleteByTargetTypeAndTargetIdIn(TargetType.COMMENT, List.of(10L));
            inOrder.verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("삭제: 작성자가 아니면 DiscussionForbiddenException이고 아무것도 지우지 않는다")
        void delete_byOther_forbidden() {
            givenComment(10L, 1L, AUTHOR);

            assertThatThrownBy(() -> service.deleteComment(OTHER, 1L, 10L))
                    .isInstanceOf(DiscussionForbiddenException.class);

            verify(commentRepository, never()).delete(any());
            verify(reactionRepository, never()).deleteByTargetTypeAndTargetIdIn(any(), any());
        }
    }

    // ===================== 반응 · 스크랩 =====================

    @Nested
    @DisplayName("반응 · 스크랩")
    class ReactionAndScrap {

        @Test
        @DisplayName("처음 반응하면 저장하고, 현재 좋아요·싫어요 수를 반환한다")
        void newReaction() {
            givenPost(1L, AUTHOR);
            given(reactionRepository.findByTargetTypeAndTargetIdAndUserId(TargetType.POST, 1L, OTHER))
                    .willReturn(Optional.empty());
            given(reactionRepository.countByTargetTypeAndTargetIdAndReactionType(TargetType.POST, 1L, ReactionType.LIKE))
                    .willReturn(3L);
            given(reactionRepository.countByTargetTypeAndTargetIdAndReactionType(TargetType.POST, 1L, ReactionType.DISLIKE))
                    .willReturn(1L);

            ReactionResponse response = service.reactToPost(OTHER, 1L, new ReactionRequest(ReactionType.LIKE));

            verify(reactionRepository).save(argThat(r -> r.getReactionType() == ReactionType.LIKE));
            assertThat(response).isEqualTo(new ReactionResponse(3, 1));
        }

        @Test
        @DisplayName("같은 반응을 다시 누르면 취소한다")
        void sameReaction_cancels() {
            givenPost(1L, AUTHOR);
            DiscussionReactionEntity existing =
                    DiscussionReactionEntity.of(TargetType.POST, 1L, OTHER, ReactionType.LIKE);
            given(reactionRepository.findByTargetTypeAndTargetIdAndUserId(TargetType.POST, 1L, OTHER))
                    .willReturn(Optional.of(existing));

            service.reactToPost(OTHER, 1L, new ReactionRequest(ReactionType.LIKE));

            verify(reactionRepository).delete(existing);
            verify(reactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("다른 반응을 누르면 기존 반응을 바꾼다")
        void differentReaction_changes() {
            givenPost(1L, AUTHOR);
            DiscussionReactionEntity existing =
                    DiscussionReactionEntity.of(TargetType.POST, 1L, OTHER, ReactionType.LIKE);
            given(reactionRepository.findByTargetTypeAndTargetIdAndUserId(TargetType.POST, 1L, OTHER))
                    .willReturn(Optional.of(existing));

            service.reactToPost(OTHER, 1L, new ReactionRequest(ReactionType.DISLIKE));

            assertThat(existing.getReactionType()).isEqualTo(ReactionType.DISLIKE);
            verify(reactionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("댓글 반응: URL의 게시글에 속하지 않은 댓글이면 반응을 저장하지 않는다")
        void commentReaction_otherPost() {
            givenComment(10L, 2L, AUTHOR);

            assertThatThrownBy(() -> service.reactToComment(OTHER, 1L, 10L, new ReactionRequest(ReactionType.LIKE)))
                    .isInstanceOf(DiscussionCommentNotFoundException.class);

            verify(reactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("댓글 반응: 처음 반응하면 댓글 대상으로 저장하고, 댓글의 좋아요·싫어요 수를 반환한다")
        void commentReaction_new() {
            givenComment(10L, 1L, AUTHOR);
            given(reactionRepository.findByTargetTypeAndTargetIdAndUserId(TargetType.COMMENT, 10L, OTHER))
                    .willReturn(Optional.empty());
            given(reactionRepository.countByTargetTypeAndTargetIdAndReactionType(TargetType.COMMENT, 10L, ReactionType.LIKE))
                    .willReturn(1L);
            given(reactionRepository.countByTargetTypeAndTargetIdAndReactionType(TargetType.COMMENT, 10L, ReactionType.DISLIKE))
                    .willReturn(0L);

            ReactionResponse response = service.reactToComment(OTHER, 1L, 10L, new ReactionRequest(ReactionType.LIKE));

            verify(reactionRepository).save(argThat(r ->
                    r.getTargetType() == TargetType.COMMENT && r.getTargetId().equals(10L)
                            && r.getReactionType() == ReactionType.LIKE));
            assertThat(response).isEqualTo(new ReactionResponse(1, 0));
        }

        @Test
        @DisplayName("스크랩: 없으면 추가하고, 있으면 해제하며 현재 스크랩 수를 반환한다")
        void toggleScrap() {
            givenPost(1L, AUTHOR);
            DiscussionScrapEntity existing = DiscussionScrapEntity.of(1L, OTHER);
            given(scrapRepository.findByPostIdAndUserId(1L, OTHER))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(existing));
            given(scrapRepository.countByPostId(1L)).willReturn(1L, 0L);

            ScrapResponse added = service.toggleScrap(OTHER, 1L);
            ScrapResponse removed = service.toggleScrap(OTHER, 1L);

            assertThat(added).isEqualTo(new ScrapResponse(1, true));
            assertThat(removed).isEqualTo(new ScrapResponse(0, false));
            verify(scrapRepository).delete(existing);
        }
    }

    // ===================== 목록 페이지 =====================

    @Nested
    @DisplayName("커서 페이지")
    class Paging {

        private List<DiscussionPostEntity> posts(int count) {
            return LongStream.rangeClosed(1, count)
                    .map(i -> 100 - i)
                    .mapToObj(id -> post(id, AUTHOR))
                    .toList();
        }

        @Test
        @DisplayName("한 페이지(20개)보다 1개 더 조회되면 20개만 담고, 마지막 게시글 ID를 다음 커서로 준다")
        void hasNext() {
            given(postRepository.findByStockCode(eq("005930"), eq(null), any())).willReturn(posts(21));

            CursorPage<PostSummary> page = service.getPostsByStock("005930", null, null);

            assertThat(page.items()).hasSize(20);
            assertThat(page.hasNext()).isTrue();
            assertThat(page.nextCursor()).isEqualTo(80L);
        }

        @Test
        @DisplayName("20개 이하로 조회되면 다음 페이지가 없다")
        void lastPage() {
            given(postRepository.findByAuthorId(eq(AUTHOR), eq(80L), any())).willReturn(posts(5));

            CursorPage<PostSummary> page = service.getMyPosts(AUTHOR, 80L);

            assertThat(page.items()).hasSize(5);
            assertThat(page.hasNext()).isFalse();
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        @DisplayName("결과가 없으면 추가 조회 없이 빈 페이지를 반환한다")
        void empty() {
            given(postRepository.findScrappedByUser(eq(AUTHOR), eq(null), any())).willReturn(List.of());

            CursorPage<PostSummary> page = service.getScrappedPosts(AUTHOR, null);

            assertThat(page.items()).isEmpty();
            assertThat(page.hasNext()).isFalse();
            verify(reactionRepository, never()).countGroupByTargetIds(any(), any());
        }

        @Test
        @DisplayName("내가 댓글 단 글 목록도 같은 방식으로 조회한다")
        void commentedOn() {
            given(postRepository.findByCommentAuthor(eq(AUTHOR), eq(null), any())).willReturn(posts(1));

            assertThat(service.getPostsICommentedOn(AUTHOR, null).items()).hasSize(1);
        }
    }

    // ===================== 묶음 조회 결과 반영 =====================

    @Nested
    @DisplayName("묶음 조회 결과 반영")
    class BatchCounts {

        private DiscussionReactionRepository.ReactionCountRow reactionRow(Long targetId, ReactionType type, long cnt) {
            DiscussionReactionRepository.ReactionCountRow row = mock(DiscussionReactionRepository.ReactionCountRow.class);
            given(row.getTargetId()).willReturn(targetId);
            given(row.getReactionType()).willReturn(type);
            given(row.getCnt()).willReturn(cnt);
            return row;
        }

        private DiscussionScrapRepository.ScrapCountRow scrapRow(Long postId, long cnt) {
            DiscussionScrapRepository.ScrapCountRow row = mock(DiscussionScrapRepository.ScrapCountRow.class);
            given(row.getPostId()).willReturn(postId);
            given(row.getCnt()).willReturn(cnt);
            return row;
        }

        private DiscussionCommentRepository.CommentCountRow commentRow(Long postId, long cnt) {
            DiscussionCommentRepository.CommentCountRow row = mock(DiscussionCommentRepository.CommentCountRow.class);
            given(row.getPostId()).willReturn(postId);
            given(row.getCnt()).willReturn(cnt);
            return row;
        }

        @Test
        @DisplayName("목록: 게시글마다 좋아요·싫어요·댓글·스크랩 수와 내 반응·스크랩 여부를 제자리에 담는다")
        void cursorPage_countsPerPost() {
            List<Long> postIds = List.of(20L, 10L);

            // 행 목은 given(...) 밖에서 먼저 만든다 (given 괄호 안에서 다른 목을 설정하면 UnfinishedStubbingException)
            List<DiscussionReactionRepository.ReactionCountRow> reactionRows = List.of(
                    reactionRow(20L, ReactionType.LIKE, 5),
                    reactionRow(20L, ReactionType.DISLIKE, 2),
                    reactionRow(10L, ReactionType.DISLIKE, 1));
            List<DiscussionScrapRepository.ScrapCountRow> scrapRows = List.of(scrapRow(20L, 3));
            List<DiscussionCommentRepository.CommentCountRow> commentRows = List.of(commentRow(10L, 7));

            given(postRepository.findByStockCode(eq("005930"), eq(null), any()))
                    .willReturn(List.of(post(20L, AUTHOR), post(10L, OTHER)));
            given(reactionRepository.countGroupByTargetIds(TargetType.POST, postIds)).willReturn(reactionRows);
            given(scrapRepository.countGroupByPostIds(postIds)).willReturn(scrapRows);
            given(commentRepository.countGroupByPostIds(postIds)).willReturn(commentRows);
            given(reactionRepository.findByTargetTypeAndTargetIdInAndUserId(TargetType.POST, postIds, AUTHOR))
                    .willReturn(List.of(DiscussionReactionEntity.of(TargetType.POST, 20L, AUTHOR, ReactionType.LIKE)));
            given(scrapRepository.findByPostIdInAndUserId(postIds, AUTHOR))
                    .willReturn(List.of(DiscussionScrapEntity.of(10L, AUTHOR)));

            List<PostSummary> items = service.getPostsByStock("005930", null, AUTHOR).items();

            PostSummary first = items.get(0);
            assertThat(first.likes()).isEqualTo(5);
            assertThat(first.dislikes()).isEqualTo(2);
            assertThat(first.scraps()).isEqualTo(3);
            assertThat(first.commentCount()).isZero();
            assertThat(first.myReaction()).isEqualTo(ReactionType.LIKE);
            assertThat(first.myScrapped()).isFalse();

            PostSummary second = items.get(1);
            assertThat(second.likes()).isZero();
            assertThat(second.dislikes()).isEqualTo(1);
            assertThat(second.scraps()).isZero();
            assertThat(second.commentCount()).isEqualTo(7);
            assertThat(second.myReaction()).isNull();
            assertThat(second.myScrapped()).isTrue();
        }

        @Test
        @DisplayName("상세: 댓글마다 좋아요·싫어요 수와 내 반응을 제자리에 담는다")
        void detail_commentCounts() {
            List<Long> commentIds = List.of(10L, 11L);
            List<DiscussionReactionRepository.ReactionCountRow> reactionRows = List.of(
                    reactionRow(10L, ReactionType.LIKE, 4),
                    reactionRow(11L, ReactionType.DISLIKE, 2));

            givenPost(1L, AUTHOR);
            given(commentRepository.findByPostIdOrderByCommentIdAsc(1L))
                    .willReturn(List.of(comment(10L, 1L, AUTHOR), comment(11L, 1L, OTHER)));
            given(reactionRepository.countGroupByTargetIds(TargetType.COMMENT, commentIds)).willReturn(reactionRows);
            given(reactionRepository.findByTargetTypeAndTargetIdInAndUserId(TargetType.COMMENT, commentIds, AUTHOR))
                    .willReturn(List.of(DiscussionReactionEntity.of(TargetType.COMMENT, 11L, AUTHOR, ReactionType.DISLIKE)));

            List<CommentResponse> comments = service.getPost(1L, AUTHOR).comments();

            assertThat(comments.get(0).likes()).isEqualTo(4);
            assertThat(comments.get(0).dislikes()).isZero();
            assertThat(comments.get(0).myReaction()).isNull();
            assertThat(comments.get(1).likes()).isZero();
            assertThat(comments.get(1).dislikes()).isEqualTo(2);
            assertThat(comments.get(1).myReaction()).isEqualTo(ReactionType.DISLIKE);
        }
    }
}
