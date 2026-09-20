package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.useraccount.dto.AccountStatus;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeverageInterestService {

    private final LeveragePositionRepository leveragePositionRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserAccountRedisRepository userAccountRedisRepository;
    private final LeverageInterestCalculator interestCalculator;
    private final AccountBalanceCommand accountBalanceCommand;

    /**
     * 이자 청구 단계.
     * 대출원금(loanAmount)은 이 단계에서 변하지 않는다(단리 방식) — 이자는 매일 현금(balance)에서 직접 차감된다.
     * 잔액이 부족해도 이자는 반드시 청구되며(신용거래의 일반 원칙), 그 결과 balance가 음수가 되면
     * NEGATIVE 상태로 전환된다(반대매매 부족분과 동일한 처리 경로 재사용).
     *
     * 이자는 연이율/365로 설계되어 있어 주말·휴장일에도 계속 발생
     * 배치가 평일(MON-FRI)에만 도는 것과 무관하게, position.lastInterestChargedDate로부터
     * 오늘까지의 "경과 달력일수"를 계산해 한 번에 몰아서 청구
     * 예) 금요일 배치 이후 다음 청구가 월요일이면 3일치(토/일/월)가 한 번에 청구
     */
    public int chargeInterestForAllPositions(List<LeveragePositionEntity> positions) {

        int charged = 0;
        LocalDate today = LocalDate.now();

        for (LeveragePositionEntity position : positions) {

            if (position.getLoanAmount() <= 0) {
                continue;
            }

            try {
                long elapsedDays = ChronoUnit.DAYS.between(position.getLastInterestChargedDate(), today);
                if (elapsedDays <= 0) {
                    continue;
                }

                long interest = interestCalculator.calculateInterest(position.getLoanAmount(), elapsedDays);
                if (interest <= 0) {
                    continue;
                }

                chargeInterestForOnePosition(position.getUsername(), position.getStockCode(),
                        position.getLeveragePositionId(), interest);

                position.markInterestChargedThrough(today);
                leveragePositionRepository.save(position);

                charged++;

            } catch (Exception e) {
                log.error("[LeverageInterest] charge failed. positionId={}, username={}, stockCode={}",
                        position.getLeveragePositionId(), position.getUsername(), position.getStockCode(), e);
            }
        }

        return charged;
    }

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
