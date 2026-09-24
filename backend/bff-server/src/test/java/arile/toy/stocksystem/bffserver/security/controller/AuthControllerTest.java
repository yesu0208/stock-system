package arile.toy.stocksystem.bffserver.security.controller;

import arile.toy.stocksystem.bffserver.security.config.JwtAuthenticationEntryPoint;
import arile.toy.stocksystem.bffserver.security.config.SecurityConfig;
import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String URL = "/api/v1/auth/refresh";
    private static final long REFRESH_VALIDITY = 7L * 24 * 60 * 60 * 1000;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;
    @MockitoBean private SlackNotifier slackNotifier;

    private final UserDetails user = User.withUsername("user1").password("pw").roles("USER").build();

    private void givenValidOldToken() {
        given(jwtService.getUsernameFromRefreshToken("old-refresh")).willReturn("user1");
        given(jwtService.getJtiFromRefreshToken("old-refresh")).willReturn("jti-old");
    }

    @Test
    @DisplayName("리프레시 토큰을 폐기하고 새 액세스·리프레시 토큰을 발급해, 리프레시 토큰은 HttpOnly 쿠키로 내려준다")
    void refresh_rotatesTokens() throws Exception {
        givenValidOldToken();
        given(refreshTokenRepository.exists("jti-old")).willReturn(true);
        given(userService.loadUserByUsername("user1")).willReturn(user);
        given(jwtService.generateAccessToken(user)).willReturn("new-access");
        given(jwtService.generateRefreshToken(user)).willReturn("new-refresh");
        given(jwtService.getJtiFromRefreshToken("new-refresh")).willReturn("jti-new");
        given(jwtService.getRefreshValidity()).willReturn(REFRESH_VALIDITY);

        mockMvc.perform(post(URL).cookie(new Cookie("refreshToken", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(cookie().value("refreshToken", "new-refresh"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/"))
                .andExpect(cookie().maxAge("refreshToken", 7 * 24 * 60 * 60));

        // 기존 토큰을 먼저 폐기한 뒤 새 토큰을 저장한다 (재사용 방지)
        InOrder inOrder = inOrder(refreshTokenRepository);
        inOrder.verify(refreshTokenRepository).delete("jti-old");
        inOrder.verify(refreshTokenRepository).save("jti-new", "user1", REFRESH_VALIDITY);
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없으면 401을 반환한다")
    void refresh_withoutCookie_401() throws Exception {
        mockMvc.perform(post(URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtService, refreshTokenRepository);
    }

    @Test
    @DisplayName("이미 폐기되었거나 로그아웃된 리프레시 토큰이면 401을 반환하고 새 토큰을 발급하지 않는다")
    void refresh_revokedToken_401() throws Exception {
        givenValidOldToken();
        given(refreshTokenRepository.exists("jti-old")).willReturn(false);

        mockMvc.perform(post(URL).cookie(new Cookie("refreshToken", "old-refresh")))
                .andExpect(status().isUnauthorized());

        verify(refreshTokenRepository, never()).delete(anyString());
        verify(refreshTokenRepository, never()).save(anyString(), anyString(), anyLong());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("만료·위조된 리프레시 토큰이면 401을 반환한다")
    void refresh_invalidToken_401() throws Exception {
        given(jwtService.getUsernameFromRefreshToken("expired-refresh"))
                .willThrow(new ExpiredJwtException(null, mock(io.jsonwebtoken.Claims.class), "expired"));

        mockMvc.perform(post(URL).cookie(new Cookie("refreshToken", "expired-refresh")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("탈퇴한 사용자면 401을 반환한다 (기존 토큰은 이미 폐기됨)")
    void refresh_userNotFound_401() throws Exception {
        givenValidOldToken();
        given(refreshTokenRepository.exists("jti-old")).willReturn(true);
        given(userService.loadUserByUsername("user1"))
                .willThrow(new arile.toy.stocksystem.bffserver.exception.user.UserNotFoundException());

        mockMvc.perform(post(URL).cookie(new Cookie("refreshToken", "old-refresh")))
                .andExpect(status().isUnauthorized());

        verify(refreshTokenRepository).delete("jti-old");
        verify(refreshTokenRepository, never()).save(anyString(), anyString(), anyLong());
    }
}
