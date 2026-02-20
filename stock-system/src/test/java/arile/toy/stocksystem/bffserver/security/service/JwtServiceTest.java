package arile.toy.stocksystem.bffserver.security.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        String secretKey = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());
        jwtService = new JwtService(secretKey);
    }

    @Test
    @DisplayName("Access 토큰 생성 후 사용자명 파싱")
    void givenUser_whenGenerateAccessToken_thenCanParseUsername() {
        // given
        var user = new User("user1", "pass", List.of());

        // when
        String token = jwtService.generateAccessToken(user);
        String username = jwtService.getUsernameFromAccessToken(token);

        // then
        assertEquals("user1", username);
    }

    @Test
    @DisplayName("Refresh 토큰 생성 후 사용자명과 JTI 파싱")
    void givenUser_whenGenerateRefreshToken_thenCanParseUsernameAndJti() {
        // given
        var user = new User("user2", "pass", List.of());

        // when
        String token = jwtService.generateRefreshToken(user);
        String username = jwtService.getUsernameFromRefreshToken(token);
        String jti = jwtService.getJtiFromRefreshToken(token);

        // then
        assertEquals("user2", username);
        assertNotNull(jti);
    }

    @Test
    @DisplayName("Access 토큰으로 Refresh 토큰 파싱 시 예외 발생")
    void givenRefreshToken_whenGetUsernameFromAccessToken_thenThrowsJwtException() {
        // given
        var user = new User("user3", "pass", List.of());
        String token = jwtService.generateRefreshToken(user); // refresh token 사용

        // when & then
        assertThrows(JwtException.class, () -> jwtService.getUsernameFromAccessToken(token));
    }

    @Test
    @DisplayName("Refresh 토큰 유효 기간 값 확인")
    void whenGetRefreshValidity_thenReturnsCorrectValue() {
        // when
        long validity = jwtService.getRefreshValidity();

        // then
        assertEquals(7 * 24 * 60 * 60 * 1000L, validity);
    }
}
