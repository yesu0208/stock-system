package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageInterestChargeExecutorTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate LAST_CHARGED = TODAY.minusDays(3);

    @Mock private LeveragePositionRepository leveragePositionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserAccountRedisRepository userAccountRedisRepository;
    @Mock private AccountBalanceCommand accountBalanceCommand;

    @InjectMocks
    private LeverageInterestChargeExecutor executor;

    private LeveragePositionEntity givenPosition() {
        LeveragePositionEntity position =
                LeveragePositionEntity.of(USERNAME, STOCK_CODE, LeverageRatio.X2, 10, 730_000L, 730_100L);
        position.setLastInterestChargedDate(LAST_CHARGED);
        given(leveragePositionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(position));
        return position;
    }

    private UserAccountEntity givenAccount(long balance, AccountStatus status) {
        UserAccountEntity account = UserAccountEntity.of(USERNAME, balance);
        if (status != AccountStatus.NORMAL) {
            account.changeAccountStatus(status, LocalDate.now().minusDays(1));
        }
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.of(account));
        return account;
    }

    private void charge() {
        executor.chargeInterestForOnePosition(USERNAME, STOCK_CODE, 1L, 255L, TODAY);
    }

    @Test
    @DisplayName("잔고와 Redis 가용현금에서 이자를 차감하고, 같은 트랜잭션에서 청구일을 갱신한다")
    void chargesInterestAndMarksChargedDate() {
        LeveragePositionEntity position = givenPosition();
        UserAccountEntity account = givenAccount(10_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        charge();

        assertThat(account.getBalance()).isEqualTo(9_745L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        verify(userAccountRepository).save(account);
        verify(accountBalanceCommand).debitAvailableCash(USERNAME, 255L);
        verifyNoInteractions(userAccountRedisRepository);

        assertThat(position.getLastInterestChargedDate()).isEqualTo(TODAY);
        verify(leveragePositionRepository).save(position);
    }

    @Test
    @DisplayName("잔고가 정확히 0이 되면 NEGATIVE로 전환하지 않는다")
    void whenBalanceBecomesZero_staysNormal() {
        givenPosition();
        UserAccountEntity account = givenAccount(255L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        charge();

        assertThat(account.getBalance()).isZero();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        verifyNoInteractions(userAccountRedisRepository);
    }

    @Test
    @DisplayName("잔고가 부족해도 이자를 청구하고, 마이너스가 되면 NORMAL 계좌를 NEGATIVE로 전환한다")
    void whenBalanceNegative_convertsToNegative() {
        givenPosition();
        UserAccountEntity account = givenAccount(100L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        charge();

        assertThat(account.getBalance()).isEqualTo(-155L);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NEGATIVE);
        verify(userAccountRedisRepository).saveAccountStatus(USERNAME, "NEGATIVE");
        verify(userAccountRepository).save(account);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AccountStatus.class, names = {"NEGATIVE", "SUSPENDED"})
    @DisplayName("이미 NORMAL이 아닌 계좌는 상태를 바꾸지 않고 이자만 청구한다")
    void whenAccountNotNormal_keepsStatus(AccountStatus status) {
        givenPosition();
        UserAccountEntity account = givenAccount(-1_000L, status);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(true);

        charge();

        assertThat(account.getBalance()).isEqualTo(-1_255L);
        assertThat(account.getAccountStatus()).isEqualTo(status);
        verifyNoInteractions(userAccountRedisRepository);
    }

    @Test
    @DisplayName("포지션이 없으면 예외를 던지고 계좌를 조회하지 않는다")
    void whenPositionNotFound_throws() {
        given(leveragePositionRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(this::charge)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Leverage position not found. id=1");

        verifyNoInteractions(userAccountRepository, accountBalanceCommand);
    }

    @Test
    @DisplayName("계좌가 없으면 예외를 던지고 청구일을 갱신하지 않는다")
    void whenAccountNotFound_throws() {
        LeveragePositionEntity position = givenPosition();
        given(userAccountRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

        assertThatThrownBy(this::charge)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Account not found. username=" + USERNAME);

        verifyNoInteractions(accountBalanceCommand);
        assertThat(position.getLastInterestChargedDate()).isEqualTo(LAST_CHARGED);
        verify(leveragePositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Redis 가용현금 차감에 실패하면 예외를 던지고 청구일을 갱신하지 않는다")
    void whenDebitFails_throws() {
        LeveragePositionEntity position = givenPosition();
        givenAccount(10_000L, AccountStatus.NORMAL);
        given(accountBalanceCommand.debitAvailableCash(USERNAME, 255L)).willReturn(false);

        assertThatThrownBy(this::charge)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis leverage interest debit failed");

        assertThat(position.getLastInterestChargedDate()).isEqualTo(LAST_CHARGED);
        verify(leveragePositionRepository, never()).save(any());
    }
}
