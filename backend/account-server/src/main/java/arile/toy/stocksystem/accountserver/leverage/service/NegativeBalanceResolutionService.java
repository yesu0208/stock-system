package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.Outcome;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NegativeBalanceResolutionService {

    private final UserAccountRepository userAccountRepository;
    private final NegativeBalanceResolutionExecutor negativeBalanceResolutionExecutor;

    /**
     * NEGATIVE 상태 계좌를 순회하며:
     *   - balance가 회복(>=0)되었으면 NORMAL로 즉시 복귀
     *   - 3영업일 경과했는데도 미회복이면 SUSPENDED로 전환
     *   - 그 외(유예 기간 내, 미회복)는 상태 유지
     */
    public ResolutionBatchResult resolveNegativeAccounts() {

        List<UserAccountEntity> negativeAccounts = userAccountRepository.findByAccountStatus(AccountStatus.NEGATIVE);

        int recovered = 0;
        int suspended = 0;
        LocalDate today = LocalDate.now();

        for (UserAccountEntity account : negativeAccounts) {
            try {
                Outcome outcome = negativeBalanceResolutionExecutor.resolveOneAccount(account.getUserAccountId(), today);
                if (outcome == Outcome.RECOVERED) recovered++;
                if (outcome == Outcome.SUSPENDED) suspended++;
            } catch (Exception e) {
                log.error("[NegativeBalanceResolution] failed. username={}", account.getUsername(), e);
            }
        }

        log.info("[NegativeBalanceResolution] batch completed. total={}, recovered={}, suspended={}",
                negativeAccounts.size(), recovered, suspended);

        return new ResolutionBatchResult(recovered, suspended);
    }

    public record ResolutionBatchResult(int recovered, int suspended) {
    }
}
