package arile.toy.stocksystem.accountserver.useraccount.entity;

import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountEntityTest {

    @Test
    @DisplayName("of: 잔고와 함께 NORMAL 상태, 마이너스 시작일 없음으로 생성한다")
    void of() {
        UserAccountEntity account = UserAccountEntity.of("user1", 1_000_000L);

        assertThat(account.getUsername()).isEqualTo("user1");
        assertThat(account.getBalance()).isEqualTo(1_000_000L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        assertThat(account.getNegativeBalanceStartDate()).isNull();
    }

    @Test
    @DisplayName("changeAccountStatus: 계좌 상태와 마이너스 시작일을 함께 바꾼다")
    void changeAccountStatus() {
        UserAccountEntity account = UserAccountEntity.of("user1", -10_000L);
        LocalDate since = LocalDate.of(2026, 9, 21);

        account.changeAccountStatus(AccountStatus.NEGATIVE, since);

        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NEGATIVE);
        assertThat(account.getNegativeBalanceStartDate()).isEqualTo(since);

        account.changeAccountStatus(AccountStatus.NORMAL, null);

        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        assertThat(account.getNegativeBalanceStartDate()).isNull();
    }

    @Test
    @DisplayName("prePersist: 생성·수정 시각을 채운다")
    void prePersist() {
        UserAccountEntity account = UserAccountEntity.of("user1", 1_000_000L);

        ReflectionTestUtils.invokeMethod(account, "prePersist");

        assertThat(account.getCreatedDateTime()).isNotNull();
        assertThat(account.getUpdatedDateTime()).isNotNull();
    }

    @Test
    @DisplayName("preUpdate: 수정 시각만 갱신한다")
    void preUpdate() {
        UserAccountEntity account = UserAccountEntity.of("user1", 1_000_000L);
        Instant created = Instant.parse("2026-09-01T00:00:00Z");
        account.setCreatedDateTime(created);
        account.setUpdatedDateTime(created);

        ReflectionTestUtils.invokeMethod(account, "preUpdate");

        assertThat(account.getCreatedDateTime()).isEqualTo(created);
        assertThat(account.getUpdatedDateTime()).isAfter(created);
    }
}
