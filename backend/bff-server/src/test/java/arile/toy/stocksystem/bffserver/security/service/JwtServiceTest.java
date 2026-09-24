package arile.toy.stocksystem.bffserver.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final byte[] KEY_BYTES = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final SecretKey KEY = Keys.hmacShaKeyFor(KEY_BYTES);
    private static final SecretKey OTHER_KEY =
            Keys.hmacShaKeyFor("fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8));

    private final JwtService jwtService = new JwtService(Base64.getEncoder().encodeToString(KEY_BYTES));

    private final UserDetails user = User.withUsername("user1").password("pw").roles("USER").build();

    private static Claims parse(String token) {
        return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    }

    /** 원하는 키·타입·만료 시각으로 토큰을 직접 만든다 (만료·위조 토큰 생성용) */
    private static String token(SecretKey key, String type, Date expiration) {
        return Jwts.builder()
                .subject("user1")
                .claim("type", type)
                .issuedAt(new Date(expiration.getTime() - 1_000))
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    private static Date past() {
        return new Date(System.currentTimeMillis() - 60_000);
    }

    private static Date future() {
        return new Date(System.currentTimeMillis() + 60_000);
    }

    @Nested
    @DisplayName("액세스 토큰")
    class AccessToken {

        @Test
        @DisplayName("발급한 토큰에서 사용자명을 꺼낸다")
        void roundTrip() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.getUsernameFromAccessToken(token)).isEqualTo("user1");
        }

        @Test
        @DisplayName("유효 기간은 15분이고 type은 access다")
        void claims() {
            Claims claims = parse(jwtService.generateAccessToken(user));

            assertThat(claims.get("type")).isEqualTo("access");
            assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                    .isEqualTo(15 * 60 * 1000L);
        }

        @Test
        @DisplayName("만료된 토큰이면 예외 대신 null을 반환한다 (재발급 유도)")
        void expired_returnsNull() {
            assertThat(jwtService.getUsernameFromAccessToken(token(KEY, "access", past()))).isNull();
        }

        @Test
        @DisplayName("리프레시 토큰을 넣으면 타입 불일치로 예외를 던진다")
        void refreshTokenGiven_throws() {
            String refresh = jwtService.generateRefreshToken(user);

            assertThatThrownBy(() -> jwtService.getUsernameFromAccessToken(refresh))
                    .isInstanceOf(JwtException.class)
                    .hasMessage("Invalid token type");
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰이면 예외를 던진다")
        void wrongSignature_throws() {
            String forged = token(OTHER_KEY, "access", future());

            assertThatThrownBy(() -> jwtService.getUsernameFromAccessToken(forged))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("형식이 깨진 토큰이면 예외를 던진다")
        void malformed_throws() {
            assertThatThrownBy(() -> jwtService.getUsernameFromAccessToken("not.a.jwt"))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("리프레시 토큰")
    class RefreshToken {

        @Test
        @DisplayName("발급한 토큰에서 사용자명과 jti를 꺼내며, 토큰마다 jti가 다르다")
        void roundTrip() {
            String first = jwtService.generateRefreshToken(user);
            String second = jwtService.generateRefreshToken(user);

            assertThat(jwtService.getUsernameFromRefreshToken(first)).isEqualTo("user1");
            assertThat(jwtService.getJtiFromRefreshToken(first))
                    .isNotBlank()
                    .isNotEqualTo(jwtService.getJtiFromRefreshToken(second));
        }

        @Test
        @DisplayName("유효 기간은 7일이고 type은 refresh다")
        void claims() {
            Claims claims = parse(jwtService.generateRefreshToken(user));

            assertThat(claims.get("type")).isEqualTo("refresh");
            assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                    .isEqualTo(jwtService.getRefreshValidity())
                    .isEqualTo(7L * 24 * 60 * 60 * 1000);
        }

        @Test
        @DisplayName("액세스 토큰을 넣으면 타입 불일치로 예외를 던진다")
        void accessTokenGiven_throws() {
            String access = jwtService.generateAccessToken(user);

            assertThatThrownBy(() -> jwtService.getUsernameFromRefreshToken(access))
                    .isInstanceOf(JwtException.class)
                    .hasMessage("Invalid token type");
        }

        @Test
        @DisplayName("만료되면 null이 아니라 예외를 던진다 (재로그인 필요)")
        void expired_throws() {
            String expired = token(KEY, "refresh", past());

            assertThatThrownBy(() -> jwtService.getUsernameFromRefreshToken(expired))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("jti가 없는 토큰(액세스 토큰)에서 jti를 꺼내면 null이다")
        void jtiOfAccessToken_isNull() {
            assertThat(jwtService.getJtiFromRefreshToken(jwtService.generateAccessToken(user))).isNull();
        }
    }
}
