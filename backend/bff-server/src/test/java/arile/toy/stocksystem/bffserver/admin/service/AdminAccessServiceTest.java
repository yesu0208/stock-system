package arile.toy.stocksystem.bffserver.admin.service;

import arile.toy.stocksystem.bffserver.exception.admin.AdminAccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccessServiceTest {

    private final AdminAccessService service = new AdminAccessService();

    private final UserDetails user = User.withUsername("user1").password("pw").roles("USER").build();
    private final UserDetails admin = User.withUsername("admin").password("pw").roles("ADMIN").build();

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        @Test
        @DisplayName("ROLE_ADMIN 권한이 있으면 관리자다")
        void admin() {
            assertThat(service.isAdmin(admin)).isTrue();
        }

        @Test
        @DisplayName("ROLE_USER만 있으면 관리자가 아니다")
        void user() {
            assertThat(service.isAdmin(user)).isFalse();
        }

        @Test
        @DisplayName("권한이 없거나 사용자 정보가 null이면 관리자가 아니다")
        void noAuthorityOrNull() {
            UserDetails noAuthority = User.withUsername("x").password("pw").authorities(new String[0]).build();

            assertThat(service.isAdmin(noAuthority)).isFalse();
            assertThat(service.isAdmin(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolveTargetUsername")
    class ResolveTargetUsername {

        @ParameterizedTest(name = "요청 username=[{0}]")
        @NullSource
        @ValueSource(strings = {"", "   ", "user1"})
        @DisplayName("대상을 지정하지 않았거나 본인이면 본인을 조회한다")
        void selfOrUnspecified_returnsSelf(String requested) {
            assertThat(service.resolveTargetUsername(user, requested)).isEqualTo("user1");
        }

        @Test
        @DisplayName("관리자는 다른 사용자를 지정해 조회할 수 있다")
        void adminRequestingOther_returnsTarget() {
            assertThat(service.resolveTargetUsername(admin, "user1")).isEqualTo("user1");
        }

        @Test
        @DisplayName("일반 사용자가 다른 사용자를 지정하면 AdminAccessDeniedException을 던진다")
        void userRequestingOther_throws() {
            assertThatThrownBy(() -> service.resolveTargetUsername(user, "other"))
                    .isInstanceOf(AdminAccessDeniedException.class);
        }
    }
}
