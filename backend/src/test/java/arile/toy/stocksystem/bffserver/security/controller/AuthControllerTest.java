package arile.toy.stocksystem.bffserver.security.controller;

import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("쿠키 없음 → 401 Unauthorized")
    void givenNoCookie_whenRefresh_thenUnauthorized() throws Exception {
        // given

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 토큰 → 401 Unauthorized")
    void givenInvalidToken_whenRefresh_thenUnauthorized() throws Exception {
        // given
        String token = "invalid-token";
        given(jwtService.getUsernameFromRefreshToken(token))
                .willThrow(new RuntimeException("Invalid token"));

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("정상 refresh token → 200 OK + 새로운 access token")
    void givenValidToken_whenRefresh_thenReturnsNewAccessToken() throws Exception {
        // given
        String oldToken = "old-refresh";
        String username = "user1";
        String jti = "jti123";
        String newAccessToken = "new-access";
        String newRefreshToken = "new-refresh";
        String newJti = "new-jti";

        given(jwtService.getUsernameFromRefreshToken(oldToken)).willReturn(username);
        given(jwtService.getJtiFromRefreshToken(oldToken)).willReturn(jti);
        given(refreshTokenRepository.exists(jti)).willReturn(true);
        given(userService.loadUserByUsername(username)).willReturn(mock(UserDetails.class));
        given(jwtService.generateAccessToken(any())).willReturn(newAccessToken);
        given(jwtService.generateRefreshToken(any())).willReturn(newRefreshToken);
        given(jwtService.getJtiFromRefreshToken(newRefreshToken)).willReturn(newJti);

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", oldToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(cookie().value("refreshToken", newRefreshToken));

        verify(refreshTokenRepository).delete(jti);
        verify(refreshTokenRepository).save(eq(newJti), eq(username), anyLong());
    }
}
