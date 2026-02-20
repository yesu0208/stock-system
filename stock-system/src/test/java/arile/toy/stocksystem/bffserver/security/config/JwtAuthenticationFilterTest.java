package arile.toy.stocksystem.bffserver.security.config;

import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("인증 제외 경로는 필터 적용 안됨")
    void givenExcludedPaths_whenShouldNotFilter_thenReturnsTrue() {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/users/authenticate");

        // then
        assert(filter.shouldNotFilter(request));

        // given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/refresh");

        // then
        assert(filter.shouldNotFilter(request));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 filterChain 호출")
    void givenNoAuthorizationHeader_whenDoFilterInternal_thenCallsFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효한 토큰이 있으면 SecurityContext에 Authentication 설정 후 filterChain 호출")
    void givenValidToken_whenDoFilterInternal_thenSetsAuthenticationAndCallsFilterChain() throws Exception {
        // given
        String token = "Bearer mytoken";
        String username = "user1";

        when(request.getHeader("Authorization")).thenReturn(token);
        when(jwtService.getUsernameFromAccessToken("mytoken")).thenReturn(username);

        var userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(List.of());

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assert(SecurityContextHolder.getContext().getAuthentication() != null);
        verify(filterChain).doFilter(request, response);
    }
}
