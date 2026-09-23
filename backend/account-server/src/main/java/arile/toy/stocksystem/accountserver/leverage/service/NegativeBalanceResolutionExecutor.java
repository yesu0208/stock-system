package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.Outcome;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 마이너스(NEGATIVE) 계좌 1건의 유예 판정 실행기.
 * NegativeBalanceResolutionService의 배치 루프에서 호출되며, 계좌마다 개별 트랜잭션으로 실행
 * (같은 클래스 내부 호출은 Spring 프록시를 거치지 않아 @Transactional이 적용되지 않으므로 별도 빈으로 분리)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NegativeBalanceResolutionExecutor {

    /** 부족분 해소 유예 기간 (영업일) */
    private static final int GRACE_PERIOD_BUSINESS_DAYS = 3;

    private final UserAccountRepository userAccountRepository;
    private final UserAccountRedisRepository userAccountRedisRepository;
    private final BusinessDayCalculator businessDayCalculator;

    @Transactional
    public Outcome resolveOneAccount(Long userAccountId, LocalDate today) {

        UserAccountEntity account = userAccountRepository.findByIdForUpdate(userAccountId)
                .orElseThrow(() -> new IllegalStateException("Account not found. id=" + userAccountId));

        if (account.getAccountStatus() != AccountStatus.NEGATIVE) {
            return Outcome.UNCHANGED; // 동시성으로 이미 처리된 경우
        }

        if (account.getBalance() >= 0) {
            account.changeAccountStatus(AccountStatus.NORMAL, null);
            userAccountRepository.save(account);
            userAccountRedisRepository.saveAccountStatus(account.getUsername(), AccountStatus.NORMAL.name());
            log.info("[NegativeBalanceResolution] account recovered. username={}, balance={}",
                    account.getUsername(), account.getBalance());
            return Outcome.RECOVERED;
        }

        int elapsed = businessDayCalculator.businessDaysElapsed(account.getNegativeBalanceStartDate(), today);

        if (elapsed >= GRACE_PERIOD_BUSINESS_DAYS) {
            account.changeAccountStatus(AccountStatus.SUSPENDED, account.getNegativeBalanceStartDate());
            userAccountRepository.save(account);
            userAccountRedisRepository.saveAccountStatus(account.getUsername(), AccountStatus.SUSPENDED.name());
            log.warn("[NegativeBalanceResolution] account suspended. username={}, balance={}, elapsedBusinessDays={}",
                    account.getUsername(), account.getBalance(), elapsed);
            return Outcome.SUSPENDED;
        }
        return Outcome.UNCHANGED;
    }
}
