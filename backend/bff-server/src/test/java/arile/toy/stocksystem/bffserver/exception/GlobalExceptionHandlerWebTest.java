package arile.toy.stocksystem.bffserver.exception;

import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerWebTest.StubController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, GlobalExceptionHandlerWebTest.StubController.class})
@ActiveProfiles("test")
class GlobalExceptionHandlerWebTest {

    @RestController
    static class StubController {
        @GetMapping("/api/v1/stub/{id}")
        String byId(@PathVariable Long id) { return "ok"; }

        @GetMapping("/api/v1/stub")
        String byParam(@RequestParam String keyword) { return "ok"; }

        @PostMapping("/api/v1/stub")
        String json(@RequestBody Map<String, Object> body) { return "ok"; }

        @GetMapping("/api/v1/stub/conflict")
        String conflict() { throw new ResponseStatusException(HttpStatus.CONFLICT, "already exists"); }

        @GetMapping("/api/v1/stub/boom")
        String boom() { throw new IllegalStateException("boom"); }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    @Test
    @DisplayName("없는 URL은 404이고 Slack 알림을 보내지 않는다")
    void unknownUrl_404() throws Exception {
        mockMvc.perform(get("/api/v1/not-exists").with(user("user1")))
                .andExpect(status().isNotFound());

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("필수 쿼리 파라미터가 없으면 400이고 Slack 알림을 보내지 않는다")
    void missingParameter_400() throws Exception {
        mockMvc.perform(get("/api/v1/stub").with(user("user1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("keyword")));

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 파라미터 이름을 담아 400으로 응답하고 Slack 알림을 보내지 않는다")
    void typeMismatch_400() throws Exception {
        mockMvc.perform(get("/api/v1/stub/abc").with(user("user1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'."));

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드면 Allow 헤더와 함께 405로 응답하고 Slack 알림을 보내지 않는다")
    void methodNotAllowed_405() throws Exception {
        mockMvc.perform(put("/api/v1/stub").with(user("user1")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type이면 415이고 Slack 알림을 보내지 않는다")
    void unsupportedMediaType_415() throws Exception {
        mockMvc.perform(post("/api/v1/stub").with(user("user1"))
                        .contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("코드에서 ResponseStatusException으로 지정한 상태 코드로 응답하고 Slack 알림을 보내지 않는다")
    void responseStatusException() throws Exception {
        mockMvc.perform(get("/api/v1/stub/conflict").with(user("user1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("already exists"));

        verifyNoInteractions(slackNotifier);
    }

    @Test
    @DisplayName("처리되지 않은 서버 오류는 여전히 500이고 Slack 알림을 보낸다")
    void unexpectedError_500() throws Exception {
        mockMvc.perform(get("/api/v1/stub/boom").with(user("user1")))
                .andExpect(status().isInternalServerError());

        verify(slackNotifier).notifyServerError(eq("/api/v1/stub/boom"), any(IllegalStateException.class));
    }
}
