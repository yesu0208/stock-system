package arile.toy.stocksystem.bffserver.security.config;

import arile.toy.stocksystem.bffserver.exception.user.UserNotFoundException;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String PROTECTED_PATH = "/api/v1/orders";

    @Mock private JwtService jwtService;
    @Mock private UserService userService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final UserDetails user = User.withUsername("user1").password("pw").roles("USER").build();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** 필터를 통과시킨 뒤 체인을 반환한다. chain.getRequest()가 null이 아니면 다음 필터로 넘어간 것 */
    private MockFilterChain run(String path, String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // ===================== 필터 적용 대상 =====================

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/api/v1/users/authenticate",
            "/api/v1/users",
            "/api/v1/users/check-username",
            "/api/v1/users/check-nickname",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/api/v1/news",
            "/api/v1/stocks/005930",
            "/uploads/profile/a.png",
            "/api/v1/chart/005930"
    })
    @DisplayName("공개 경로는 토큰이 있어도 검사하지 않고 통과시킨다")
    void publicPaths_skipFilter(String path) throws Exception {
        MockFilterChain chain = run(path, "Bearer some-token");

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(jwtService, userService);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 없이 통과시킨다")
    void noHeader_passesWithoutAuth() throws Exception {
        MockFilterChain chain = run(PROTECTED_PATH, null);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 인증 없이 통과시킨다")
    void notBearer_passesWithoutAuth() throws Exception {
        MockFilterChain chain = run(PROTECTED_PATH, "Basic dXNlcjpwdw==");

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    // ===================== 정상 인증 =====================

    @Test
    @DisplayName("유효한 액세스 토큰이면 사용자 정보와 권한으로 인증을 설정하고 통과시킨다")
    void validToken_authenticates() throws Exception {
        given(jwtService.getUsernameFromAccessToken("valid-token")).willReturn("user1");
        given(userService.loadUserByUsername("user1")).willReturn(user);

        MockFilterChain chain = run(PROTECTED_PATH, "Bearer valid-token");

        assertThat(chain.getRequest()).isNotNull();
        Authentication authentication = currentAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
    }

    @Test
    @DisplayName("이미 인증된 요청이면 사용자를 다시 조회하지 않는다")
    void alreadyAuthenticated_skipsLoad() throws Exception {
        UsernamePasswordAuthenticationToken existing =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existing);
        given(jwtService.getUsernameFromAccessToken("valid-token")).willReturn("user1");

        MockFilterChain chain = run(PROTECTED_PATH, "Bearer valid-token");

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentAuthentication()).isSameAs(existing);
        verifyNoInteractions(userService);
    }

    // ===================== 인증 실패 → 인증 없이 통과 (이후 401) =====================

    @Test
    @DisplayName("만료된 토큰이면 인증 없이 통과시킨다 (이후 401 → 프론트가 재발급)")
    void expiredToken_passesWithoutAuth() throws Exception {
        given(jwtService.getUsernameFromAccessToken("expired-token")).willReturn(null);

        MockFilterChain chain = run(PROTECTED_PATH, "Bearer expired-token");

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(userService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"jwt", "blank", "userNotFound"})
    @DisplayName("위조·손상된 토큰, 빈 토큰, 탈퇴한 사용자면 예외 없이 인증 없이 통과시켜 이후 401 흐름으로 넘긴다")
    void invalidToken_passesWithoutAuth(String failure) throws Exception {
        switch (failure) {
            case "jwt" -> given(jwtService.getUsernameFromAccessToken("bad-token"))
                    .willThrow(new JwtException("invalid signature"));
            case "blank" -> given(jwtService.getUsernameFromAccessToken("bad-token"))
                    .willThrow(new IllegalArgumentException("CharSequence cannot be null or empty."));
            case "userNotFound" -> {
                given(jwtService.getUsernameFromAccessToken("bad-token")).willReturn("deleted-user");
                given(userService.loadUserByUsername("deleted-user")).willThrow(new UserNotFoundException());
            }
        }

        MockFilterChain chain = run(PROTECTED_PATH, "Bearer bad-token");

        assertThat(chain.getRequest()).isNotNull();
        assertThat(currentAuthentication()).isNull();
    }

    @Test
    @DisplayName("예외가 나면 이전에 남아 있던 인증 정보도 지운다")
    void invalidToken_clearsContext() throws Exception {
        UsernamePasswordAuthenticationToken stale =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(stale);
        given(jwtService.getUsernameFromAccessToken("bad-token")).willThrow(new JwtException("invalid"));

        run(PROTECTED_PATH, "Bearer bad-token");

        assertThat(currentAuthentication()).isNull();
    }
}
