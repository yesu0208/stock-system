package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 레버리지 포지션 1건의 이자 청구 실행기.
 * LeverageInterestService의 배치 루프에서 호출되며, 포지션마다 개별 트랜잭션으로 실행
 * (같은 클래스 내부 호출은 Spring 프록시를 거치지 않아 @Transactional이 적용되지 않으므로 별도 빈으로 분리)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeverageInterestChargeExecutor {

    private final UserAccountRepository userAccountRepository;
    private final UserAccountRedisRepository userAccountRedisRepository;
    private final AccountBalanceCommand accountBalanceCommand;

    @Transactional
    public void chargeInterestForOnePosition(String username, String stockCode, Long positionId, long interestAmount) {

        UserAccountEntity account = userAccountRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new IllegalStateException("Account not found. username=" + username));

        account.setBalance(account.getBalance() - interestAmount);

        boolean wasNormal = account.getAccountStatus() == AccountStatus.NORMAL;
        if (account.getBalance() < 0 && wasNormal) {
            account.changeAccountStatus(AccountStatus.NEGATIVE, LocalDate.now());
            userAccountRedisRepository.saveAccountStatus(username, AccountStatus.NEGATIVE.name());
            log.warn("[LeverageInterest] Account converted to NEGATIVE by interest charge. username={}, balance={}",
                    username, account.getBalance());
        }

        userAccountRepository.save(account);

        boolean debited = accountBalanceCommand.debitAvailableCash(username, interestAmount);
        if (!debited) {
            log.error("Redis availableCash debit failed for leverage interest. username={}, stockCode={}, interest={}",
                    username, stockCode, interestAmount);
            throw new IllegalStateException(
                    "Redis leverage interest debit failed. username=%s, stockCode=%s".formatted(username, stockCode));
        }

        log.info("[LeverageInterest] charged. username={}, stockCode={}, positionId={}, interestAmount={}, balanceAfter={}",
                username, stockCode, positionId, interestAmount, account.getBalance());
    }
}
