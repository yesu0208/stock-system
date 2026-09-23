package arile.toy.stocksystem.bffserver.user.service;

import arile.toy.stocksystem.bffserver.exception.user.NicknameAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.user.PasswordMismatchException;
import arile.toy.stocksystem.bffserver.exception.user.UserAlreadyExistsException;
import arile.toy.stocksystem.bffserver.exception.user.UserNotFoundException;
import arile.toy.stocksystem.bffserver.security.repository.RefreshTokenRepository;
import arile.toy.stocksystem.bffserver.security.service.JwtService;
import arile.toy.stocksystem.bffserver.user.dto.ChangeNicknameRequest;
import arile.toy.stocksystem.bffserver.user.dto.ChangePasswordRequest;
import arile.toy.stocksystem.bffserver.user.dto.Role;
import arile.toy.stocksystem.bffserver.user.dto.UserAuthenticationResponse;
import arile.toy.stocksystem.bffserver.user.dto.UserDto;
import arile.toy.stocksystem.bffserver.user.dto.UserLoginRequest;
import arile.toy.stocksystem.bffserver.user.dto.UserSignUpRequest;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import arile.toy.stocksystem.bffserver.user.event.publisher.UserCreatedEventPublisher;
import arile.toy.stocksystem.bffserver.user.notifier.SlackNotifier;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import arile.toy.stocksystem.bffserver.user.storage.ProfileImageStorage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USERNAME = "user1";
    private static final long REFRESH_VALIDITY = 7L * 24 * 60 * 60 * 1000;

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserCreatedEventPublisher userCreatedEventPublisher;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private ProfileImageStorage profileImageStorage;
    @Mock private SlackNotifier slackNotifier;

    @InjectMocks
    private UserService userService;

    private static UserEntity user(String nickname) {
        UserEntity entity = UserEntity.of(USERNAME, "encoded-pw", nickname);
        entity.setUserId(1L);
        entity.setCreatedDateTime(Instant.parse("2026-09-01T00:00:00Z"));
        return entity;
    }

    private UserEntity givenUser() {
        UserEntity entity = user("닉네임");
        given(userRepository.findByUsername(USERNAME)).willReturn(Optional.of(entity));
        return entity;
    }

    private void givenNoUser() {
        given(userRepository.findByUsername(USERNAME)).willReturn(Optional.empty());
    }

    // ===================== 조회 =====================

    @Nested
    @DisplayName("사용자 조회")
    class Find {

        @Test
        @DisplayName("loadUserByUsername: 사용자 엔티티를 UserDetails로 반환한다")
        void loadUserByUsername() {
            UserEntity entity = givenUser();

            assertThat(userService.loadUserByUsername(USERNAME)).isSameAs(entity);
        }

        @Test
        @DisplayName("loadUserByUsername: 없으면 아이디를 담은 UserNotFoundException을 던진다")
        void loadUserByUsername_notFound() {
            givenNoUser();

            assertThatThrownBy(() -> userService.loadUserByUsername(USERNAME))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(USERNAME);
        }

        @Test
        @DisplayName("getUserByUsername: 사용자 정보를 DTO로 반환한다")
        void getUserByUsername() {
            givenUser();

            UserDto dto = userService.getUserByUsername(USERNAME);

            assertThat(dto.username()).isEqualTo(USERNAME);
            assertThat(dto.nickname()).isEqualTo("닉네임");
            assertThat(dto.role()).isEqualTo(Role.USER);
            assertThat(dto.rank()).isNull();
        }

        @Test
        @DisplayName("getUserByUsername: 없으면 UserNotFoundException을 던진다")
        void getUserByUsername_notFound() {
            givenNoUser();

            assertThatThrownBy(() -> userService.getUserByUsername(USERNAME))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("getAllUsers: 전체 사용자를 DTO 목록으로 반환한다")
        void getAllUsers() {
            given(userRepository.findAll()).willReturn(List.of(user("a"), user("b")));

            assertThat(userService.getAllUsers()).extracting(UserDto::nickname).containsExactly("a", "b");
        }

        @Test
        @DisplayName("isUsernameExists, isNicknameExists: 중복 여부를 반환한다")
        void existence() {
            given(userRepository.findByUsername("taken")).willReturn(Optional.of(user("n")));
            given(userRepository.findByUsername("free")).willReturn(Optional.empty());
            given(userRepository.existsByNickname("taken")).willReturn(true);
            given(userRepository.existsByNickname("free")).willReturn(false);

            assertThat(userService.isUsernameExists("taken")).isTrue();
            assertThat(userService.isUsernameExists("free")).isFalse();
            assertThat(userService.isNicknameExists("taken")).isTrue();
            assertThat(userService.isNicknameExists("free")).isFalse();
        }
    }

    // ===================== 회원가입 =====================

    @Nested
    @DisplayName("signUp")
    class SignUp {

        private final UserSignUpRequest request = new UserSignUpRequest(USERNAME, "닉네임", "password1!");

        private void givenAvailable() {
            givenNoUser();
            given(userRepository.existsByNickname("닉네임")).willReturn(false);
            given(bCryptPasswordEncoder.encode("password1!")).willReturn("encoded-pw");
            given(userRepository.save(any(UserEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("비밀번호를 암호화해 USER 권한으로 저장하고, 계좌 생성 이벤트와 Slack 알림을 보낸다")
        void signUp() {
            givenAvailable();

            UserDto dto = userService.signUp(request);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            UserEntity saved = captor.getValue();
            assertThat(saved.getUsername()).isEqualTo(USERNAME);
            assertThat(saved.getNickname()).isEqualTo("닉네임");
            assertThat(saved.getPassword()).isEqualTo("encoded-pw");
            assertThat(saved.getRole()).isEqualTo(Role.USER);

            verify(userCreatedEventPublisher).publishUserCreatedEvent(UserCreatedEvent.of(USERNAME));
            verify(slackNotifier).notifySignUp(USERNAME, "닉네임", null);
            assertThat(dto.username()).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("Slack 알림이 실패해도 회원가입은 성공한다")
        void signUp_whenSlackFails_stillSucceeds() {
            givenAvailable();
            willThrow(new RuntimeException("slack down"))
                    .given(slackNotifier).notifySignUp(anyString(), anyString(), any());

            assertThatCode(() -> userService.signUp(request)).doesNotThrowAnyException();

            verify(userCreatedEventPublisher).publishUserCreatedEvent(UserCreatedEvent.of(USERNAME));
        }

        @Test
        @DisplayName("아이디가 이미 있으면 UserAlreadyExistsException을 던지고 저장하지 않는다")
        void duplicateUsername_throws() {
            given(userRepository.findByUsername(USERNAME)).willReturn(Optional.of(user("기존")));

            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
            verifyNoInteractions(userCreatedEventPublisher, slackNotifier);
        }

        @Test
        @DisplayName("닉네임이 이미 있으면 NicknameAlreadyExistsException을 던지고 저장하지 않는다")
        void duplicateNickname_throws() {
            givenNoUser();
            given(userRepository.existsByNickname("닉네임")).willReturn(true);

            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(NicknameAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
            verifyNoInteractions(userCreatedEventPublisher, slackNotifier);
        }
    }

    // ===================== 로그인 =====================

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        private final UserLoginRequest request = new UserLoginRequest(USERNAME, "password1!");

        @Test
        @DisplayName("비밀번호가 맞으면 액세스 토큰을 반환하고, 리프레시 토큰을 저장한 뒤 HttpOnly 쿠키로 내려준다")
        void success() {
            UserEntity entity = givenUser();
            given(bCryptPasswordEncoder.matches("password1!", "encoded-pw")).willReturn(true);
            given(jwtService.generateAccessToken(entity)).willReturn("access");
            given(jwtService.generateRefreshToken(entity)).willReturn("refresh");
            given(jwtService.getJtiFromRefreshToken("refresh")).willReturn("jti-1");
            given(jwtService.getRefreshValidity()).willReturn(REFRESH_VALIDITY);
            MockHttpServletResponse response = new MockHttpServletResponse();

            UserAuthenticationResponse result = userService.authenticate(request, response);

            assertThat(result).isEqualTo(new UserAuthenticationResponse("access"));
            verify(refreshTokenRepository).save("jti-1", USERNAME, REFRESH_VALIDITY);

            Cookie cookie = response.getCookie("refreshToken");
            assertThat(cookie).isNotNull();
            assertThat(cookie.getValue()).isEqualTo("refresh");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 UserNotFoundException을 던지고 토큰을 발급하지 않는다")
        void wrongPassword_throws() {
            givenUser();
            given(bCryptPasswordEncoder.matches("password1!", "encoded-pw")).willReturn(false);
            MockHttpServletResponse response = new MockHttpServletResponse();

            assertThatThrownBy(() -> userService.authenticate(request, response))
                    .isInstanceOf(UserNotFoundException.class);

            verifyNoInteractions(jwtService, refreshTokenRepository);
            assertThat(response.getCookie("refreshToken")).isNull();
        }

        @Test
        @DisplayName("없는 아이디면 UserNotFoundException을 던진다")
        void unknownUser_throws() {
            givenNoUser();

            assertThatThrownBy(() -> userService.authenticate(request, new MockHttpServletResponse()))
                    .isInstanceOf(UserNotFoundException.class);

            verifyNoInteractions(bCryptPasswordEncoder, jwtService);
        }
    }

    // ===================== 정보 변경 =====================

    @Nested
    @DisplayName("정보 변경")
    class Change {

        @Test
        @DisplayName("changePassword: 현재 비밀번호가 맞으면 새 비밀번호를 암호화해 바꾼다")
        void changePassword() {
            UserEntity entity = givenUser();
            given(bCryptPasswordEncoder.matches("old-pw", "encoded-pw")).willReturn(true);
            given(bCryptPasswordEncoder.encode("new-pw1!")).willReturn("encoded-new");

            userService.changePassword(USERNAME, new ChangePasswordRequest("old-pw", "new-pw1!"));

            assertThat(entity.getPassword()).isEqualTo("encoded-new");
        }

        @Test
        @DisplayName("changePassword: 현재 비밀번호가 틀리면 PasswordMismatchException을 던지고 바꾸지 않는다")
        void changePassword_mismatch() {
            UserEntity entity = givenUser();
            given(bCryptPasswordEncoder.matches("wrong", "encoded-pw")).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword(USERNAME, new ChangePasswordRequest("wrong", "new-pw1!")))
                    .isInstanceOf(PasswordMismatchException.class);

            assertThat(entity.getPassword()).isEqualTo("encoded-pw");
        }

        @Test
        @DisplayName("changeNickname: 사용 가능한 닉네임이면 바꾼다")
        void changeNickname() {
            UserEntity entity = givenUser();
            given(userRepository.existsByNickname("새닉")).willReturn(false);

            UserDto dto = userService.changeNickname(USERNAME, new ChangeNicknameRequest("새닉"));

            assertThat(entity.getNickname()).isEqualTo("새닉");
            assertThat(dto.nickname()).isEqualTo("새닉");
        }

        @Test
        @DisplayName("changeNickname: 현재와 같은 닉네임이면 중복 확인 없이 그대로 둔다")
        void changeNickname_same() {
            givenUser();

            UserDto dto = userService.changeNickname(USERNAME, new ChangeNicknameRequest("닉네임"));

            assertThat(dto.nickname()).isEqualTo("닉네임");
            verify(userRepository, never()).existsByNickname(anyString());
        }

        @Test
        @DisplayName("changeNickname: 다른 사람이 쓰는 닉네임이면 NicknameAlreadyExistsException을 던진다")
        void changeNickname_taken() {
            UserEntity entity = givenUser();
            given(userRepository.existsByNickname("남의닉")).willReturn(true);

            assertThatThrownBy(() -> userService.changeNickname(USERNAME, new ChangeNicknameRequest("남의닉")))
                    .isInstanceOf(NicknameAlreadyExistsException.class);

            assertThat(entity.getNickname()).isEqualTo("닉네임");
        }

        @Test
        @DisplayName("changeProfileImage: 이미지를 저장하고 반환된 URL로 프로필 이미지를 바꾼다")
        void changeProfileImage() {
            UserEntity entity = givenUser();
            MockMultipartFile file = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});
            given(profileImageStorage.store(file, USERNAME)).willReturn("/uploads/profile/user1.png");

            UserDto dto = userService.changeProfileImage(USERNAME, file);

            assertThat(entity.getProfileImageUrl()).isEqualTo("/uploads/profile/user1.png");
            assertThat(dto.profileImageUrl()).isEqualTo("/uploads/profile/user1.png");
        }
    }
}
