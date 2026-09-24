package arile.toy.stocksystem.bffserver.discussion.controller;

import arile.toy.stocksystem.bffserver.discussion.dto.CommentCreateRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.CommentEditRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.CursorPage;
import arile.toy.stocksystem.bffserver.discussion.dto.PostCreateRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.PostEditRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.ReactionRequest;
import arile.toy.stocksystem.bffserver.discussion.dto.ReactionResponse;
import arile.toy.stocksystem.bffserver.discussion.dto.ScrapResponse;
import arile.toy.stocksystem.bffserver.discussion.entity.ReactionType;
import arile.toy.stocksystem.bffserver.discussion.service.DiscussionService;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionForbiddenException;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionPostNotFoundException;
import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiscussionController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class DiscussionControllerTest {

    private static final String BASE = "/api/v1/discussions";
    private static final CursorPage<?> EMPTY_PAGE = new CursorPage<>(List.of(), null, false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DiscussionService discussionService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    // ===================== 인증 =====================

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "GET,    /api/v1/discussions/1",
            "GET,    /api/v1/discussions/stocks/005930",
            "GET,    /api/v1/discussions/my/posts",
            "GET,    /api/v1/discussions/my/commented",
            "GET,    /api/v1/discussions/my/scraps",
            "DELETE, /api/v1/discussions/1",
            "POST,   /api/v1/discussions/1/scrap",
            "DELETE, /api/v1/discussions/1/comments/10"
    })
    @DisplayName("로그인하지 않으면 조회를 포함한 모든 토론 API가 401이다")
    void unauthenticated_401(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(discussionService);
    }

    // ===================== 게시글 =====================

    @Nested
    @DisplayName("게시글")
    class Post {

        @Test
        @DisplayName("POST /discussions: 로그인 사용자를 작성자로 넘기고 201을 반환한다")
        void create() throws Exception {
            mockMvc.perform(post(BASE).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                            {"stockCode": "005930", "stockName": "삼성전자", "title": "제목", "content": "내용"}
                            """))
                    .andExpect(status().isCreated());

            verify(discussionService).createPost(eq("user1"),
                    eq(new PostCreateRequest("005930", "삼성전자", "제목", "내용")));
        }

        @Test
        @DisplayName("POST /discussions: 필수값이 비어 있으면 400이고 저장하지 않는다")
        void create_invalid() throws Exception {
            mockMvc.perform(post(BASE).with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(discussionService);
        }

        @Test
        @DisplayName("GET /discussions/{postId}: 로그인 사용자를 조회자로 넘긴다 (내 반응·내 스크랩 표시용)")
        void getPost() throws Exception {
            mockMvc.perform(get(BASE + "/1").with(user("user1")))
                    .andExpect(status().isOk());

            verify(discussionService).getPost(1L, "user1");
        }

        @Test
        @DisplayName("GET /discussions/{postId}: 없는 게시글이면 예외에 정의된 상태 코드로 응답한다")
        void getPost_notFound() throws Exception {
            DiscussionPostNotFoundException notFound = new DiscussionPostNotFoundException(1L);
            given(discussionService.getPost(1L, "user1")).willThrow(notFound);

            mockMvc.perform(get(BASE + "/1").with(user("user1")))
                    .andExpect(status().is(notFound.getStatus().value()));
        }

        @Test
        @DisplayName("PATCH /discussions/{postId}: 로그인 사용자를 수정 요청자로 넘긴다")
        void edit() throws Exception {
            mockMvc.perform(patch(BASE + "/1").with(user("user1")).contentType(MediaType.APPLICATION_JSON).content("""
                            {"title": "새 제목", "content": "새 내용"}
                            """))
                    .andExpect(status().isOk());

            verify(discussionService).editPost("user1", 1L, new PostEditRequest("새 제목", "새 내용"));
        }

        @Test
        @DisplayName("PATCH /discussions/{postId}: 작성자가 아니면 예외에 정의된 상태 코드로 응답한다")
        void edit_forbidden() throws Exception {
            DiscussionForbiddenException forbidden = new DiscussionForbiddenException();
            given(discussionService.editPost(eq("other"), eq(1L), any())).willThrow(forbidden);

            mockMvc.perform(patch(BASE + "/1").with(user("other")).contentType(MediaType.APPLICATION_JSON).content("""
                            {"title": "탈취", "content": "탈취"}
                            """))
                    .andExpect(status().is(forbidden.getStatus().value()));
        }

        @Test
        @DisplayName("DELETE /discussions/{postId}: 로그인 사용자를 삭제 요청자로 넘기고 204를 반환한다")
        void deletePost() throws Exception {
            mockMvc.perform(delete(BASE + "/1").with(user("user1")))
                    .andExpect(status().isNoContent());

            verify(discussionService).deletePost("user1", 1L);
        }

        @Test
        @DisplayName("DELETE /discussions/{postId}: 작성자가 아니면 예외에 정의된 상태 코드로 응답한다")
        void deletePost_forbidden() throws Exception {
            DiscussionForbiddenException forbidden = new DiscussionForbiddenException();
            willThrow(forbidden).given(discussionService).deletePost("other", 1L);

            mockMvc.perform(delete(BASE + "/1").with(user("other")))
                    .andExpect(status().is(forbidden.getStatus().value()));
        }
    }

    // ===================== 목록 =====================

    @Nested
    @DisplayName("목록")
    class Lists {

        @Test
        @DisplayName("종목별 목록: 커서와 로그인 사용자를 넘긴다")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void byStock() throws Exception {
            given(discussionService.getPostsByStock("005930", 80L, "user1")).willReturn((CursorPage) EMPTY_PAGE);

            mockMvc.perform(get(BASE + "/stocks/005930").param("cursor", "80").with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("내 글·내가 댓글 단 글·스크랩한 글: 로그인 사용자 기준으로 조회한다 (첫 페이지는 커서 없음)")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void myLists() throws Exception {
            given(discussionService.getMyPosts("user1", null)).willReturn((CursorPage) EMPTY_PAGE);
            given(discussionService.getPostsICommentedOn("user1", null)).willReturn((CursorPage) EMPTY_PAGE);
            given(discussionService.getScrappedPosts("user1", null)).willReturn((CursorPage) EMPTY_PAGE);

            mockMvc.perform(get(BASE + "/my/posts").with(user("user1"))).andExpect(status().isOk());
            mockMvc.perform(get(BASE + "/my/commented").with(user("user1"))).andExpect(status().isOk());
            mockMvc.perform(get(BASE + "/my/scraps").with(user("user1"))).andExpect(status().isOk());

            verify(discussionService).getMyPosts("user1", null);
            verify(discussionService).getPostsICommentedOn("user1", null);
            verify(discussionService).getScrappedPosts("user1", null);
        }
    }

    // ===================== 댓글 · 반응 · 스크랩 =====================

    @Nested
    @DisplayName("댓글 · 반응 · 스크랩")
    class Interactions {

        @Test
        @DisplayName("댓글 작성: 로그인 사용자를 작성자로 넘기고 201을 반환한다")
        void addComment() throws Exception {
            mockMvc.perform(post(BASE + "/1/comments").with(user("user1"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"댓글\"}"))
                    .andExpect(status().isCreated());

            verify(discussionService).addComment("user1", 1L, new CommentCreateRequest("댓글"));
        }

        @Test
        @DisplayName("댓글 작성: 내용이 비어 있으면 400이다")
        void addComment_blank() throws Exception {
            mockMvc.perform(post(BASE + "/1/comments").with(user("user1"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"  \"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(discussionService);
        }

        @Test
        @DisplayName("댓글 수정·삭제: 로그인 사용자와 게시글·댓글 ID를 넘긴다")
        void editAndDeleteComment() throws Exception {
            mockMvc.perform(patch(BASE + "/1/comments/10").with(user("user1"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"수정\"}"))
                    .andExpect(status().isOk());
            mockMvc.perform(delete(BASE + "/1/comments/10").with(user("user1")))
                    .andExpect(status().isNoContent());

            verify(discussionService).editComment("user1", 1L, 10L, new CommentEditRequest("수정"));
            verify(discussionService).deleteComment("user1", 1L, 10L);
        }

        @Test
        @DisplayName("게시글·댓글 반응: 반응 결과(좋아요·싫어요 수)를 반환한다")
        void reactions() throws Exception {
            given(discussionService.reactToPost("user1", 1L, new ReactionRequest(ReactionType.LIKE)))
                    .willReturn(new ReactionResponse(3, 1));
            given(discussionService.reactToComment("user1", 1L, 10L, new ReactionRequest(ReactionType.DISLIKE)))
                    .willReturn(new ReactionResponse(0, 2));

            mockMvc.perform(post(BASE + "/1/reactions").with(user("user1"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\": \"LIKE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.likes").value(3));
            mockMvc.perform(post(BASE + "/1/comments/10/reactions").with(user("user1"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\": \"DISLIKE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dislikes").value(2));
        }

        @Test
        @DisplayName("스크랩: 토글 결과(스크랩 수·스크랩 여부)를 반환한다")
        void scrap() throws Exception {
            given(discussionService.toggleScrap("user1", 1L)).willReturn(new ScrapResponse(5, true));

            mockMvc.perform(post(BASE + "/1/scrap").with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scraps").value(5))
                    .andExpect(jsonPath("$.scrapped").value(true));
        }
    }

    // ===================== 방어 코드 =====================

    @Test
    @DisplayName("[방어 코드] 인증 주체가 null이면 모든 API가 401을 반환한다 (SecurityConfig에 막혀 실제로는 도달하지 않음)")
    void nullPrincipal_401() {
        DiscussionController controller = new DiscussionController(discussionService);
        ReactionRequest like = new ReactionRequest(ReactionType.LIKE);

        assertThat(controller.createPost(null, new PostCreateRequest("005930", "삼성전자", "제목", "내용"))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.getPost(null, 1L).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.editPost(null, 1L, new PostEditRequest("t", "c")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.deletePost(null, 1L).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.getPostsByStock(null, "005930", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.getMyPosts(null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.getPostsICommentedOn(null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.getScrappedPosts(null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.reactToPost(null, 1L, like).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.toggleScrap(null, 1L).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.addComment(null, 1L, new CommentCreateRequest("c")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.editComment(null, 1L, 10L, new CommentEditRequest("c")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.deleteComment(null, 1L, 10L).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.reactToComment(null, 1L, 10L, like).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(discussionService);
    }
}
