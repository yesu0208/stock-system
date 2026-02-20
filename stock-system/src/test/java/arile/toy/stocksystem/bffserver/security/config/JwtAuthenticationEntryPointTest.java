package arile.toy.stocksystem.bffserver.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthenticationEntryPointTest {

    @Test
    @DisplayName("인증 실패 시 401 Unauthorized 전송")
    void givenUnauthenticatedRequest_whenCommence_thenSendsUnauthorized() throws IOException {
        // given
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);

        // when
        entryPoint.commence(request, response, authException);

        // then
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
