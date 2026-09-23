package arile.toy.stocksystem.bffserver.user.entity;

import arile.toy.stocksystem.bffserver.user.dto.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    @DisplayName("of: USER 권한으로 생성한다")
    void of() {
        UserEntity user = UserEntity.of("user1", "encoded-pw", "닉네임");

        assertThat(user.getUsername()).isEqualTo("user1");
        assertThat(user.getPassword()).isEqualTo("encoded-pw");
        assertThat(user.getNickname()).isEqualTo("닉네임");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("getAuthorities: USER는 ROLE_USER, ADMIN은 ROLE_ADMIN 권한을 가진다")
    void authorities() {
        UserEntity user = UserEntity.of("user1", "pw", "닉네임");
        UserEntity admin = UserEntity.of("admin", "pw", "관리자");
        admin.setRole(Role.ADMIN);

        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(admin.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("계정 만료·잠금·자격 만료 없이 항상 활성 상태다")
    void accountFlags() {
        UserEntity user = UserEntity.of("user1", "pw", "닉네임");

        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("비밀번호·닉네임·프로필 이미지를 변경한다")
    void changes() {
        UserEntity user = UserEntity.of("user1", "pw", "닉네임");

        user.changePassword("new-pw");
        user.changeNickname("새닉");
        user.changeProfileImageUrl("/uploads/profile/user1.png");

        assertThat(user.getPassword()).isEqualTo("new-pw");
        assertThat(user.getNickname()).isEqualTo("새닉");
        assertThat(user.getProfileImageUrl()).isEqualTo("/uploads/profile/user1.png");
    }

    @Test
    @DisplayName("prePersist: 가입 시각을 기록한다")
    void prePersist() {
        UserEntity user = UserEntity.of("user1", "pw", "닉네임");

        ReflectionTestUtils.invokeMethod(user, "prePersist");

        assertThat(user.getCreatedDateTime()).isNotNull();
    }
}
