package arile.toy.stocksystem.bffserver.stomp.config;

import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StompJwtChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompJwtChannelInterceptor interceptor;

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }


    @Test
    @DisplayName("JWT가 없을 때 CONNECT 시도하면 JwtException 발생")
    void givenMissingJwt_whenConnect_thenThrowJwtException() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When & Then
        JwtException ex = assertThrows(JwtException.class,
                () -> interceptor.preSend(message, channel));
        assertEquals("Authorization failed: JWT is missing or invalid.", ex.getMessage());
    }

    @Test
    @DisplayName("만료된 JWT로 CONNECT 시도하면 JwtException 발생")
    void givenExpiredJwt_whenConnect_thenThrowJwtException() {
        // Given
        String token = "expiredToken";
        when(jwtService.getUsernameFromAccessToken(token)).thenReturn(null);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When & Then
        JwtException ex = assertThrows(JwtException.class,
                () -> interceptor.preSend(message, channel));
        assertEquals("Authorization failed: JWT is expired.", ex.getMessage());
    }

    @Test
    @DisplayName("CONNECT 외 명령은 preSend 후 메시지 변경 없음")
    void givenNonConnectCommand_whenPreSend_thenMessageUnchanged() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, channel);

        // Then
        assertSame(message, result);
    }
}
