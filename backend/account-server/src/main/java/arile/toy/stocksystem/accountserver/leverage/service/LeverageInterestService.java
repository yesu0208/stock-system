package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageInterestService {

    private final LeveragePositionRepository leveragePositionRepository;
    private final UserAccountRepository userAccountRepository;
    private final LeverageInterestCalculator interestCalculator;
    private final AccountBalanceCommand accountBalanceCommand;

    /**
     * 이자 청구 단계.
     * 대출원금(loanAmount)은 이 단계에서 변하지 않는다(단리 방식) — 이자는 매일 현금(balance)에서 직접 차감된다.
     * 잔액이 부족해도 이자는 반드시 청구되며(신용거래의 일반 원칙), 그 결과 balance가 음수가 되면
     * NEGATIVE 상태로 전환된다(반대매매 부족분과 동일한 처리 경로 재사용).
     */
    public int chargeInterestForAllPositions(List<LeveragePositionEntity> positions) {

        int charged = 0;

        for (LeveragePositionEntity position : positions) {

            if (position.getLoanAmount() <= 0) {
                continue;
            }

            try {
                long dailyInterest = interestCalculator.calculateDailyInterest(position.getLoanAmount());
                if (dailyInterest <= 0) {
                    continue;
                }

                chargeInterestForOnePosition(position.getUsername(), position.getStockCode(),
                        position.getLeveragePositionId(), dailyInterest);
                charged++;

            } catch (Exception e) {
                log.error("[LeverageInterest] charge failed. positionId={}, username={}, stockCode={}",
                        position.getLeveragePositionId(), position.getUsername(), position.getStockCode(), e);
            }
        }

        return charged;
    }

    @Transactional
    public void chargeInterestForOnePosition(String username, String stockCode, Long positionId, long dailyInterest) {

        UserAccountEntity account = userAccountRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new IllegalStateException("Account not found. username=" + username));

        account.setBalance(account.getBalance() - dailyInterest);

        boolean wasNormal = account.getAccountStatus() == AccountStatus.NORMAL;
        if (account.getBalance() < 0 && wasNormal) {
            account.changeAccountStatus(AccountStatus.NEGATIVE, LocalDate.now());
            log.warn("[LeverageInterest] Account converted to NEGATIVE by interest charge. username={}, balance={}",
                    username, account.getBalance());
        }

        userAccountRepository.save(account);

        boolean debited = accountBalanceCommand.debitAvailableCash(username, dailyInterest);
        if (!debited) {
            log.error("Redis availableCash debit failed for leverage interest. username={}, stockCode={}, interest={}",
                    username, stockCode, dailyInterest);
            throw new IllegalStateException(
                    "Redis leverage interest debit failed. username=%s, stockCode=%s".formatted(username, stockCode));
        }

        log.info("[LeverageInterest] charged. username={}, stockCode={}, positionId={}, dailyInterest={}, balanceAfter={}",
                username, stockCode, positionId, dailyInterest, account.getBalance());
    }
}
