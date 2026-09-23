package arile.toy.stocksystem.accountserver.useraccount.service;

import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountStatusGuardTest {

    private static final String USERNAME = "user1";

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AccountStatusGuard accountStatusGuard;

    private void givenAccountStatus(AccountStatus status) {
        UserAccountEntity account = UserAccountEntity.of(USERNAME, 10_000L);
        account.changeAccountStatus(status, LocalDate.now());
        given(userAccountRepository.findByUsername(USERNAME)).willReturn(Optional.of(account));
    }

    @ParameterizedTest(name = "{0} → 매수 허용={1}")
    @CsvSource({"NORMAL, true", "NEGATIVE, false", "SUSPENDED, false"})
    @DisplayName("allowBuy: NORMAL 상태에서만 매수를 허용한다")
    void allowBuy(AccountStatus status, boolean expected) {
        givenAccountStatus(status);

        assertThat(accountStatusGuard.allowBuy(USERNAME)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → 매도 허용={1}")
    @CsvSource({"NORMAL, true", "NEGATIVE, true", "SUSPENDED, false"})
    @DisplayName("allowSell: SUSPENDED 상태에서만 매도를 금지한다")
    void allowSell(AccountStatus status, boolean expected) {
        givenAccountStatus(status);

        assertThat(accountStatusGuard.allowSell(USERNAME)).isEqualTo(expected);
    }

    @Test
    @DisplayName("계좌가 없으면 NORMAL로 간주해 매수·매도를 모두 허용한다")
    void whenAccountNotFound_treatsAsNormal() {
        given(userAccountRepository.findByUsername(USERNAME)).willReturn(Optional.empty());

        assertThat(accountStatusGuard.allowBuy(USERNAME)).isTrue();
        assertThat(accountStatusGuard.allowSell(USERNAME)).isTrue();
    }
}
