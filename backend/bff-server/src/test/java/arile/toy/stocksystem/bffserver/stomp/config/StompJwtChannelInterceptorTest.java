package arile.toy.stocksystem.bffserver.stomp.config;

import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StompJwtChannelInterceptorTest {

    @Mock private JwtService jwtService;
    @Mock private UserService userService;

    @InjectMocks
    private StompJwtChannelInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);
    private final UserDetails user = User.withUsername("user1").password("pw").roles("USER").build();

    private static Message<byte[]> message(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static StompHeaderAccessor accessorOf(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }

    @Test
    @DisplayName("CONNECT가 아닌 명령은 인증 없이 그대로 통과시킨다")
    void nonConnect_passes() {
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, "Bearer token");

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(jwtService, userService);
    }

    @Test
    @DisplayName("CONNECT에 토큰이 없으면 익명 연결로 허용한다 (공개 채널 구독용)")
    void connectWithoutToken_allowsAnonymous() {
        Message<byte[]> message = message(StompCommand.CONNECT, null);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        assertThat(accessorOf(result).getUser()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("CONNECT의 Authorization이 Bearer 형식이 아니면 익명 연결로 허용한다")
    void connectWithNonBearer_allowsAnonymous() {
        Message<byte[]> message = message(StompCommand.CONNECT, "Basic abc");

        assertThat(accessorOf(interceptor.preSend(message, channel)).getUser()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("CONNECT에 유효한 토큰이 있으면 세션 사용자로 인증 정보를 설정한다")
    void connectWithValidToken_setsUser() {
        given(jwtService.getUsernameFromAccessToken("valid-token")).willReturn("user1");
        given(userService.loadUserByUsername("user1")).willReturn(user);

        Message<?> result = interceptor.preSend(message(StompCommand.CONNECT, "Bearer valid-token"), channel);

        assertThat(accessorOf(result).getUser())
                .isInstanceOfSatisfying(UsernamePasswordAuthenticationToken.class, auth -> {
                    assertThat(auth.getPrincipal()).isEqualTo(user);
                    assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
                });
    }

    @Test
    @DisplayName("CONNECT의 토큰이 만료되었으면 연결을 거부한다")
    void connectWithExpiredToken_rejects() {
        given(jwtService.getUsernameFromAccessToken("expired-token")).willReturn(null);

        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.CONNECT, "Bearer expired-token"), channel))
                .isInstanceOf(JwtException.class)
                .hasMessage("Authorization failed: JWT is expired.");

        verifyNoInteractions(userService);
    }
}
