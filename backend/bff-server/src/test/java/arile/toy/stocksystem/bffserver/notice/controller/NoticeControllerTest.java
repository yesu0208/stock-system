package arile.toy.stocksystem.bffserver.notice.controller;

import arile.toy.stocksystem.bffserver.exception.notice.NoticeNotFoundException;
import arile.toy.stocksystem.bffserver.notice.dto.CursorPage;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeCreateRequest;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeDetail;
import arile.toy.stocksystem.bffserver.notice.dto.NoticeEditRequest;
import arile.toy.stocksystem.bffserver.notice.service.NoticeService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

@WebMvcTest({NoticeController.class, NoticeAdminController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class NoticeControllersTest {

    private static final String ADMIN_BASE = "/api/v1/admin/notices";
    private static final String BASE = "/api/v1/notices";
    private static final NoticeDetail DETAIL = new NoticeDetail(1L, "점검 안내", "내일 점검합니다.", "admin",
            Instant.parse("2026-09-24T00:00:00Z"), Instant.parse("2026-09-24T00:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private NoticeService noticeService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    private static String body(String title, String content) {
        return "{\"title\": \"%s\", \"content\": \"%s\"}".formatted(title, content);
    }

    // ===================== 권한 =====================

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"POST, /api/v1/admin/notices", "PATCH, /api/v1/admin/notices/1", "DELETE, /api/v1/admin/notices/1"})
    @DisplayName("관리자 API: 일반 사용자는 403, 비로그인은 401이며 아무것도 처리하지 않는다")
    void adminApis_forbiddenForNonAdmin(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).with(user("user1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("t", "c")))
                .andExpect(status().isForbidden());
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON).content(body("t", "c")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(noticeService);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"/api/v1/notices", "/api/v1/notices/1"})
    @DisplayName("공지 조회: 로그인하지 않으면 401이다")
    void userApis_requireLogin(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());

        verifyNoInteractions(noticeService);
    }

    // ===================== 관리자 =====================

    @Nested
    @DisplayName("관리자")
    class AdminApis {

        @Test
        @DisplayName("작성: 로그인한 관리자를 작성자로 넘기고 201을 반환한다")
        void create() throws Exception {
            given(noticeService.createNotice("admin", new NoticeCreateRequest("점검 안내", "내일 점검합니다.")))
                    .willReturn(DETAIL);

            mockMvc.perform(post(ADMIN_BASE).with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(body("점검 안내", "내일 점검합니다.")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.noticeId").value(1))
                    .andExpect(jsonPath("$.authorId").value("admin"));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "제목 비어 있음 | {\"title\": \" \", \"content\": \"내용\"}",
                "내용 없음     | {\"title\": \"제목\"}"
        })
        @DisplayName("작성: 제목·내용이 비어 있으면 400이다")
        void create_blank(String description, String json) throws Exception {
            mockMvc.perform(post(ADMIN_BASE).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(noticeService);
        }

        @Test
        @DisplayName("작성·수정: 제목 255자·내용 4000자를 넘으면 400이다 (DB 오류로 가지 않음)")
        void tooLong() throws Exception {
            String longTitle = "가".repeat(256);
            String longContent = "가".repeat(4001);

            mockMvc.perform(post(ADMIN_BASE).with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(body(longTitle, "내용")))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(patch(ADMIN_BASE + "/1").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(body("제목", longContent)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(noticeService);
        }

        @Test
        @DisplayName("작성: 제목 255자·내용 4000자는 허용한다")
        void maxLength_allowed() throws Exception {
            given(noticeService.createNotice(eq("admin"), org.mockito.ArgumentMatchers.any())).willReturn(DETAIL);

            mockMvc.perform(post(ADMIN_BASE).with(admin()).contentType(MediaType.APPLICATION_JSON)
                            .content(body("가".repeat(255), "가".repeat(4000))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("수정: 공지 ID와 새 내용을 넘긴다")
        void edit() throws Exception {
            given(noticeService.editNotice(1L, new NoticeEditRequest("새 제목", "새 내용"))).willReturn(DETAIL);

            mockMvc.perform(patch(ADMIN_BASE + "/1").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(body("새 제목", "새 내용")))
                    .andExpect(status().isOk());

            verify(noticeService).editNotice(1L, new NoticeEditRequest("새 제목", "새 내용"));
        }

        @Test
        @DisplayName("삭제: 204를 반환한다")
        void deleteNotice() throws Exception {
            mockMvc.perform(delete(ADMIN_BASE + "/1").with(admin()))
                    .andExpect(status().isNoContent());

            verify(noticeService).deleteNotice(1L);
        }
    }

    // ===================== 사용자 =====================

    @Nested
    @DisplayName("사용자")
    class UserApis {

        @Test
        @DisplayName("목록: 커서를 넘겨 공지 목록을 반환한다")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void list() throws Exception {
            given(noticeService.getNotices(80L)).willReturn((CursorPage) new CursorPage<>(List.of(), null, false));

            mockMvc.perform(get(BASE).param("cursor", "80").with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("상세: 공지를 반환하고, 없으면 예외에 정의된 상태 코드로 응답한다")
        void detail() throws Exception {
            given(noticeService.getNotice(1L)).willReturn(DETAIL);
            NoticeNotFoundException notFound = new NoticeNotFoundException(2L);
            given(noticeService.getNotice(2L)).willThrow(notFound);

            mockMvc.perform(get(BASE + "/1").with(user("user1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("점검 안내"));
            mockMvc.perform(get(BASE + "/2").with(user("user1")))
                    .andExpect(status().is(notFound.getStatus().value()));
        }
    }
}
