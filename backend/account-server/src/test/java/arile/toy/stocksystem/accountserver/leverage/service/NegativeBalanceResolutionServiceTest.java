package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.Outcome;
import arile.toy.stocksystem.accountserver.leverage.service.NegativeBalanceResolutionService.ResolutionBatchResult;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NegativeBalanceResolutionServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private NegativeBalanceResolutionExecutor negativeBalanceResolutionExecutor;

    @InjectMocks
    private NegativeBalanceResolutionService service;

    private static UserAccountEntity account(long id) {
        UserAccountEntity account = UserAccountEntity.of("user" + id, -10_000L);
        account.changeAccountStatus(AccountStatus.NEGATIVE, LocalDate.now().minusDays(5));
        ReflectionTestUtils.setField(account, "userAccountId", id);
        return account;
    }

    @Test
    @DisplayName("NEGATIVE 계좌가 없으면 판정하지 않는다")
    void whenNoNegativeAccounts_doesNothing() {
        given(userAccountRepository.findByAccountStatus(AccountStatus.NEGATIVE)).willReturn(List.of());

        assertThat(service.resolveNegativeAccounts()).isEqualTo(new ResolutionBatchResult(0, 0));

        verifyNoInteractions(negativeBalanceResolutionExecutor);
    }

    @Test
    @DisplayName("계좌별 판정 결과를 집계하고, 실패한 계좌는 집계에서 제외한 채 계속 진행한다")
    void aggregatesAndContinuesOnFailure() {
        LocalDate today = LocalDate.now();
        given(userAccountRepository.findByAccountStatus(AccountStatus.NEGATIVE))
                .willReturn(List.of(account(1L), account(2L), account(3L), account(4L), account(5L)));
        given(negativeBalanceResolutionExecutor.resolveOneAccount(1L, today)).willReturn(Outcome.RECOVERED);
        given(negativeBalanceResolutionExecutor.resolveOneAccount(2L, today)).willReturn(Outcome.SUSPENDED);
        given(negativeBalanceResolutionExecutor.resolveOneAccount(3L, today)).willReturn(Outcome.UNCHANGED);
        given(negativeBalanceResolutionExecutor.resolveOneAccount(4L, today))
                .willThrow(new IllegalStateException("Account not found. id=4"));
        given(negativeBalanceResolutionExecutor.resolveOneAccount(5L, today)).willReturn(Outcome.RECOVERED);

        ResolutionBatchResult result = service.resolveNegativeAccounts();

        assertThat(result).isEqualTo(new ResolutionBatchResult(2, 1));
        verify(negativeBalanceResolutionExecutor).resolveOneAccount(5L, today);
    }
}
