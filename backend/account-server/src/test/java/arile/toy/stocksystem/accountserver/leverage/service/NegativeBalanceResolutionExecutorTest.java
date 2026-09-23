package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.Outcome;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
class NegativeBalanceResolutionExecutorTest {

    private static final String USERNAME = "user1";
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final LocalDate NEGATIVE_SINCE = LocalDate.of(2026, 9, 21);

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserAccountRedisRepository userAccountRedisRepository;
    @Mock private BusinessDayCalculator businessDayCalculator;

    @InjectMocks
    private NegativeBalanceResolutionExecutor executor;

    private UserAccountEntity givenAccount(long balance, AccountStatus status) {
        UserAccountEntity account = UserAccountEntity.of(USERNAME, balance);
        if (status != AccountStatus.NORMAL) {
            account.changeAccountStatus(status, NEGATIVE_SINCE);
        }
        given(userAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(account));
        return account;
    }

    @ParameterizedTest(name = "잔고 {0}")
    @ValueSource(longs = {0L, 1_000L})
    @DisplayName("잔고가 회복(0 이상)되었으면 NORMAL로 복귀하고 마이너스 시작일을 지운다")
    void whenRecovered_returnsToNormal(long balance) {
        UserAccountEntity account = givenAccount(balance, AccountStatus.NEGATIVE);

        Outcome outcome = executor.resolveOneAccount(1L, TODAY);

        assertThat(outcome).isEqualTo(Outcome.RECOVERED);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NORMAL);
        assertThat(account.getNegativeBalanceStartDate()).isNull();
        verify(userAccountRepository).save(account);
        verify(userAccountRedisRepository).saveAccountStatus(USERNAME, "NORMAL");
        verifyNoInteractions(businessDayCalculator);
    }

    @ParameterizedTest(name = "경과 {0}영업일")
    @ValueSource(ints = {3, 4})
    @DisplayName("유예 기간(3영업일)이 지나도 미회복이면 마이너스 시작일을 유지한 채 SUSPENDED로 전환한다")
    void whenGraceExpired_suspends(int elapsed) {
        UserAccountEntity account = givenAccount(-10_000L, AccountStatus.NEGATIVE);
        given(businessDayCalculator.businessDaysElapsed(NEGATIVE_SINCE, TODAY)).willReturn(elapsed);

        Outcome outcome = executor.resolveOneAccount(1L, TODAY);

        assertThat(outcome).isEqualTo(Outcome.SUSPENDED);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(account.getNegativeBalanceStartDate()).isEqualTo(NEGATIVE_SINCE);
        verify(userAccountRepository).save(account);
        verify(userAccountRedisRepository).saveAccountStatus(USERNAME, "SUSPENDED");
    }

    @ParameterizedTest(name = "경과 {0}영업일")
    @ValueSource(ints = {0, 2})
    @DisplayName("유예 기간 내 미회복이면 상태를 유지하고 저장하지 않는다")
    void whenWithinGrace_unchanged(int elapsed) {
        UserAccountEntity account = givenAccount(-10_000L, AccountStatus.NEGATIVE);
        given(businessDayCalculator.businessDaysElapsed(NEGATIVE_SINCE, TODAY)).willReturn(elapsed);

        Outcome outcome = executor.resolveOneAccount(1L, TODAY);

        assertThat(outcome).isEqualTo(Outcome.UNCHANGED);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.NEGATIVE);
        verify(userAccountRepository, never()).save(any());
        verifyNoInteractions(userAccountRedisRepository);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AccountStatus.class, names = {"NORMAL", "SUSPENDED"})
    @DisplayName("그 사이 NEGATIVE가 아니게 된 계좌는 건드리지 않는다")
    void whenNotNegative_unchanged(AccountStatus status) {
        UserAccountEntity account = givenAccount(-10_000L, status);

        Outcome outcome = executor.resolveOneAccount(1L, TODAY);

        assertThat(outcome).isEqualTo(Outcome.UNCHANGED);
        assertThat(account.getAccountStatus()).isEqualTo(status);
        verify(userAccountRepository, never()).save(any());
        verifyNoInteractions(userAccountRedisRepository, businessDayCalculator);
    }

    @Test
    @DisplayName("계좌가 없으면 예외를 던진다")
    void whenAccountNotFound_throws() {
        given(userAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> executor.resolveOneAccount(1L, TODAY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Account not found. id=1");
    }
}
