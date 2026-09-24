package arile.toy.stocksystem.bffserver.security.config;

import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.StubController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, SecurityConfigTest.StubController.class})
@ActiveProfiles("test")
class SecurityConfigTest {

    /** 인가 규칙만 확인하기 위한 가짜 컨트롤러. 보안을 통과하면 "ok"를 반환한다 */
    @RestController
    static class StubController {
        @PostMapping({"/api/v1/users", "/api/v1/users/authenticate", "/api/v1/auth/refresh"})
        String publicPost() { return "ok"; }

        @GetMapping({"/api/v1/users/check-username", "/api/v1/users/check-nickname", "/api/v1/news",
                "/api/v1/stocks/005930", "/api/v1/stocks/market/popular", "/api/v1/market/phase",
                "/actuator/health", "/api/v1/users/all", "/api/v1/admin/holidays", "/api/v1/orders"})
        String get() { return "ok"; }

        @PostMapping("/api/v1/orders")
        String protectedPost() { return "ok"; }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private SlackNotifier slackNotifier;

    // ===================== 공개 경로 =====================

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "POST, /api/v1/users",
            "POST, /api/v1/users/authenticate",
            "POST, /api/v1/auth/refresh",
            "GET, /api/v1/users/check-username",
            "GET, /api/v1/users/check-nickname",
            "GET, /api/v1/market/phase",
            "GET, /actuator/health"
    })
    @DisplayName("공개 경로는 인증 없이 접근할 수 있다")
    void publicPaths_permitAll(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    // ===================== 보호된 경로 =====================

    @Nested
    @DisplayName("인증이 필요한 경로")
    class Protected {

        @Test
        @DisplayName("토큰 없이 요청하면 401을 반환한다")
        void withoutToken_401() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/api/v1/news", "/api/v1/stocks/005930", "/api/v1/stocks/market/popular"})
        @DisplayName("뉴스·시세 정보는 로그인 사용자 전용이라 토큰 없이 요청하면 401, 로그인하면 접근할 수 있다")
        void externalDataApis_requireLogin(String path) throws Exception {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get(path).with(user("user1")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("공개 GET 경로라도 다른 메서드(POST)면 인증이 필요하다")
        void publicPathWithOtherMethod_401() throws Exception {
            mockMvc.perform(request(HttpMethod.POST, "/api/v1/users/check-username"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("유효한 토큰이면 접근할 수 있다")
        void withValidToken_200() throws Exception {
            given(jwtService.getUsernameFromAccessToken("valid-token")).willReturn("user1");
            given(userService.loadUserByUsername("user1"))
                    .willReturn(User.withUsername("user1").password("pw").roles("USER").build());

            mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("만료된 토큰이면 401을 반환한다 (프론트 재발급 트리거)")
        void withExpiredToken_401() throws Exception {
            given(jwtService.getUsernameFromAccessToken("expired-token")).willReturn(null);

            mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("위조·손상된 토큰이면 500이 아니라 401을 반환한다")
        void withForgedToken_401() throws Exception {
            given(jwtService.getUsernameFromAccessToken("forged-token")).willThrow(new JwtException("invalid"));

            mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer forged-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("공개 경로에 위조 토큰이 붙어 있어도 익명으로 정상 처리한다")
        void publicPathWithForgedToken_200() throws Exception {
            given(jwtService.getUsernameFromAccessToken("forged-token")).willThrow(new JwtException("invalid"));

            mockMvc.perform(get("/api/v1/market/phase").header(HttpHeaders.AUTHORIZATION, "Bearer forged-token"))
                    .andExpect(status().isOk());
        }
    }

    // ===================== 관리자 경로 =====================

    @Nested
    @DisplayName("관리자 전용 경로")
    class Admin {

        @ParameterizedTest(name = "{0}")
        @CsvSource({"/api/v1/users/all", "/api/v1/admin/holidays"})
        @DisplayName("일반 사용자는 403을 반환한다")
        void userRole_403(String path) throws Exception {
            mockMvc.perform(get(path).with(user("user1").roles("USER")))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"/api/v1/users/all", "/api/v1/admin/holidays"})
        @DisplayName("관리자는 접근할 수 있다")
        void adminRole_200(String path) throws Exception {
            mockMvc.perform(get(path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("인증 없이 요청하면 403이 아니라 401을 반환한다")
        void anonymous_401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/holidays"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===================== CORS =====================

    @Nested
    @DisplayName("CORS")
    class Cors {

        @Test
        @DisplayName("프론트엔드 도메인의 preflight 요청을 허용하고 쿠키 전송을 허용한다")
        void allowedOrigin() throws Exception {
            mockMvc.perform(options("/api/v1/orders")
                            .header(HttpHeaders.ORIGIN, "test-fe-url")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "test-fe-url"))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }

        @Test
        @DisplayName("허용되지 않은 도메인의 preflight 요청은 거부한다")
        void disallowedOrigin() throws Exception {
            mockMvc.perform(options("/api/v1/orders")
                            .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isForbidden());
        }
    }
}
