package arile.toy.stocksystem.bffserver.stomp.config;

import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.service.UserService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new JwtException("Authorization failed: JWT is missing or invalid.");
            }

            String accessToken = authHeader.substring(7);
            String username = jwtService.getUsername(accessToken);
            var userDetails = userService.loadUserByUsername(username);

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            accessor.setUser(authentication);
        }

        return message;
    }
}
