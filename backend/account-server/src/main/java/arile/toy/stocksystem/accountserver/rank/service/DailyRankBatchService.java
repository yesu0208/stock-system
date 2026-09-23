package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.service.DailyRankExecutor.DailyRankOutcome;
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
public class DailyRankBatchService {

    private final UserAccountRepository userAccountRepository;
    private final DailyRankExecutor dailyRankExecutor;

    /**
     * 트랜잭션은 사용자 단위로 DailyRankExecutor에서 개별 적용
     * (이 메서드에 @Transactional을 두면 실행기 트랜잭션이 합류해 사용자 단위 분리가 무의미해짐)
     */
    public void runDailyBatch() {

        LocalDate today = LocalDate.now();

        List<UserAccountEntity> accounts = userAccountRepository.findAll();
        int processed = 0;

        for (UserAccountEntity account : accounts) {
            try {
                DailyRankOutcome outcome =
                        dailyRankExecutor.processOneUser(account.getUsername(), account.getBalance(), today);
                if (outcome == DailyRankOutcome.RANKED) {
                    processed++;
                }
            } catch (Exception e) {
                log.error("Daily rank batch failed for username={}", account.getUsername(), e);
            }
        }

        log.info("[DailyRankBatch] completed. total={}, processed={}", accounts.size(), processed);
    }
}
