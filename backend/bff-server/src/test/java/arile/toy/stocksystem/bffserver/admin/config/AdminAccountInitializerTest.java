package arile.toy.stocksystem.bffserver.admin.config;

import arile.toy.stocksystem.bffserver.user.dto.Role;
import arile.toy.stocksystem.bffserver.user.entity.UserEntity;
import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import arile.toy.stocksystem.bffserver.user.event.publisher.UserCreatedEventPublisher;
import arile.toy.stocksystem.bffserver.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock private UserCreatedEventPublisher userCreatedEventPublisher;

    private AdminAccountProperties properties;
    private AdminAccountInitializer initializer;

    @BeforeEach
    void setUp() {
        properties = new AdminAccountProperties();
        properties.setUsername("admin");
        properties.setPassword("admin-password!1");
        initializer = new AdminAccountInitializer(
                properties, userRepository, bCryptPasswordEncoder, userCreatedEventPublisher);
    }

    @ParameterizedTest(name = "username=[{0}], password=[{1}]")
    @CsvSource(value = {"NULL, pw", "'', pw", "'  ', pw", "admin, NULL", "admin, ''"}, nullValues = "NULL")
    @DisplayName("관리자 아이디나 비밀번호 설정이 비어 있으면 계정도 계좌도 만들지 않는다")
    void blankSettings_skips(String username, String password) {
        properties.setUsername(username);
        properties.setPassword(password);

        initializer.run(null);

        verifyNoInteractions(userRepository, bCryptPasswordEncoder, userCreatedEventPublisher);
    }

    @Test
    @DisplayName("관리자 계정이 없으면 ADMIN 권한과 기본 닉네임으로 생성하고 계좌 생성을 요청한다")
    void createsAdmin() {
        given(userRepository.findByUsername("admin")).willReturn(Optional.empty());
        given(bCryptPasswordEncoder.encode("admin-password!1")).willReturn("encoded");

        initializer.run(null);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getNickname()).isEqualTo("관리자");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);

        verify(userCreatedEventPublisher).publishUserCreatedEvent(UserCreatedEvent.of("admin"));
    }

    @Test
    @DisplayName("같은 아이디의 일반 계정이 있으면 비밀번호는 그대로 두고 ADMIN으로 승격하며, 계좌 생성도 요청한다")
    void promotesExistingUser() {
        UserEntity existing = UserEntity.of("admin", "user-chosen-pw", "기존닉");
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(existing));

        initializer.run(null);

        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
        assertThat(existing.getPassword()).isEqualTo("user-chosen-pw");
        verify(userRepository).save(existing);
        verifyNoInteractions(bCryptPasswordEncoder);
        verify(userCreatedEventPublisher).publishUserCreatedEvent(UserCreatedEvent.of("admin"));
    }

    @Test
    @DisplayName("이미 관리자 계정이면 저장하지 않지만, 계좌 생성은 다시 요청한다 (기존 환경의 계좌 누락 복구)")
    void existingAdmin_requestsAccountAgain() {
        UserEntity existing = UserEntity.of("admin", "pw", "관리자");
        existing.setRole(Role.ADMIN);
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(existing));

        initializer.run(null);

        verify(userRepository, never()).save(any());
        verify(userCreatedEventPublisher).publishUserCreatedEvent(UserCreatedEvent.of("admin"));
    }

    @Test
    @DisplayName("계좌 생성 요청 발행에 실패해도 예외를 던지지 않아 서버 시작을 막지 않는다")
    void publishFails_doesNotBlockStartup() {
        given(userRepository.findByUsername("admin")).willReturn(Optional.empty());
        given(bCryptPasswordEncoder.encode("admin-password!1")).willReturn("encoded");
        willThrow(new RedisConnectionFailureException("redis down"))
                .given(userCreatedEventPublisher).publishUserCreatedEvent(any());

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();

        verify(userRepository).save(any(UserEntity.class));
    }
}
