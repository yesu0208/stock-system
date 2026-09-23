package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageInterestChargeExecutorTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserAccountRedisRepository userAccountRedisRepository;
    @Mock private AccountBalanceCommand accountBalanceCommand;

    @InjectMocks
    private LeverageInterestChargeExecutor executor;

    private UserAccountEntity givenAccount(long balance, AccountStatus status) {
        UserAccountEntity account = UserAccountEntity.of(USERNAME, balance);
        if (status != AccountStatus.NORMAL) {
            account.changeAccountStatus(status, LocalDate.now().minusDays(1));
        }
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.of(account));
        return account;
    }

    @Test
    @DisplayName("잔고에서 이자를 차감하고 Redis 가용현금에서도 차감한다")
    void chargesInterest() {
        UserAccountEntity account = givenAccount(10_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        executor.chargeInterestForOnePosition(USERNAME, STOCK_CODE, 1L, 255L);

        assertThat(account.getBalance()).isEqualTo(9_745L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        verify(userAccountRepository).save(account);
        verify(accountBalanceCommand).debitAvailableCash(USERNAME, 255L);
        verifyNoInteractions(userAccountRedisRepository);
    }

    @Test
    @DisplayName("잔고가 정확히 0이 되면 NEGATIVE로 전환하지 않는다")
    void whenBalanceBecomesZero_staysNormal() {
        UserAccountEntity account = givenAccount(255L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        executor.chargeInterestForOnePosition(USERNAME, STOCK_CODE, 1L, 255L);

        assertThat(account.getBalance()).isZero();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        verifyNoInteractions(userAccountRedisRepository);
    }

    @Test
    @DisplayName("잔고가 부족해도 이자를 청구하고, 마이너스가 되면 NORMAL 계좌를 NEGATIVE로 전환한다")
    void whenBalanceNegative_convertsToNegative() {
        UserAccountEntity account = givenAccount(100L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        executor.chargeInterestForOnePosition(USERNAME, STOCK_CODE, 1L, 255L);

        assertThat(account.getBalance()).isEqualTo(-155L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NEGATIVE);
        verify(userAccountRedisRepository).saveAccountStatus(USERNAME, "NEGATIVE");
        verify(userAccountRepository).save(account);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AccountStatus.class, names = {"NEGATIVE", "SUSPENDED"})
    @DisplayName("이미 NORMAL이 아닌 계좌는 상태를 바꾸지 않고 이자만 청구한다")
    void whenAccountNotNormal_keepsStatus(AccountStatus status) {
        UserAccountEntity account = givenAccount(-1_000L, status);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        executor.chargeInterestForOnePosition(USERNAME, STOCK_CODE, 1L, 255L);

        assertThat(account.getBalance()).isEqualTo(-1_255L);
        assertThat(account.getAccountStatus()).isEqualTo(status);
        verifyNoInteractions(userAccountRedisRepository);
    }

    @Test
    @DisplayName("계좌가 없으면 예외를 던진다")
    void whenAccountNotFound_throws() {
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

        assertThatThrownBy(() -> executor.chargeInterestForOnePosition(USERNAME, STOCK_CODE, 1L, 255L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Account not found. username=" + USERNAME);

        verifyNoInteractions(accountBalanceCommand);
    }

    @Test
    @DisplayName("Redis 가용현금 차감에 실패하면 예외를 던진다")
    void whenDebitFails_throws() {
        givenAccount(10_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(false);

        assertThatThrownBy(() -> executor.chargeInterestForOnePosition(USERNAME, STOCK_CODE, 1L, 255L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis leverage interest debit failed");
    }
}
