package arile.toy.stocksystem.bffserver.user.service;

import arile.toy.stocksystem.bffserver.exception.user.UserAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.user.UserNotFoundException;
import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.UserAuthenticationResponse;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.dto.UserLoginRequest;
import arile.toy.stocksystem.bffserver.user.dto.UserSignUpRequest;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.event.publisher.UserCreatedEventPublisher;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserCreatedEventPublisher userCreatedEventPublisher;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("새로운 사용자 가입 시 User 저장 및 USER_CREATED 이벤트 발행")
    void givenNewUser_whenSignUp_thenUserSavedAndEventPublished() {
        // Given
        var request = new UserSignUpRequest("user1", "password123");
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        when(bCryptPasswordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDto result = userService.signUp(request);

        // Then
        assertEquals("user1", result.username());
        verify(userRepository).save(any(UserEntity.class));
        verify(userCreatedEventPublisher).publishUserCreatedEvent(any());
    }

    @Test
    @DisplayName("이미 존재하는 사용자로 가입 시 UserAlreadyExistsException 발생")
    void givenExistingUser_whenSignUp_thenThrowException() {
        // Given
        var request = new UserSignUpRequest("user1", "password123");
        when(userRepository.findByUsername(request.username()))
                .thenReturn(Optional.of(mock(UserEntity.class)));

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> userService.signUp(request));
    }

    @Test
    @DisplayName("정확한 자격 증명으로 인증 시 AccessToken 반환 및 쿠키 추가")
    void givenCorrectCredentials_whenAuthenticate_thenAccessTokenReturnedAndCookieAdded() {
        // Given
        String username = "user1";
        String rawPassword = "password123";
        UserEntity user = UserEntity.of(username, "encodedPassword");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches(rawPassword, "encodedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");
        when(jwtService.getJtiFromRefreshToken("refreshToken")).thenReturn("jti");
        when(jwtService.getRefreshValidity()).thenReturn(7 * 24 * 60 * 60 * 1000L); // 7일

        // When
        UserAuthenticationResponse result = userService.authenticate(
                new UserLoginRequest(username, rawPassword),
                response
        );

        // Then
        assertEquals("accessToken", result.accessToken());
        verify(refreshTokenRepository).save("jti", username, jwtService.getRefreshValidity());
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 인증 시 UserNotFoundException 발생")
    void givenWrongPassword_whenAuthenticate_thenThrowException() {
        // Given
        String username = "user1";
        UserEntity user = UserEntity.of(username, "encodedPassword");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> userService.authenticate(new UserLoginRequest(username, "wrongPassword"), response));
    }

    @Test
    @DisplayName("존재하지 않는 username으로 loadUserByUsername 호출 시 UserNotFoundException 발생")
    void givenNonExistingUsername_whenLoadUser_thenThrowException() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.loadUserByUsername("unknown"));
    }
}
