package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyRankBatchService {

    private final UserAccountRepository userAccountRepository;
    private final UserRankRepository userRankRepository;
    private final TotalAssetCalculator totalAssetCalculator;
    private final RankScoreCalculator rankScoreCalculator;
    private final RankDecisionService rankDecisionService;

    @Transactional
    public void runDailyBatch() {

        List<UserAccountEntity> accounts = userAccountRepository.findAll();
        int processed = 0;

        for (UserAccountEntity account : accounts) {

            String username = account.getUsername();

            UserRankEntity rank = userRankRepository.findByUsername(username).orElse(null);
            if (rank == null) {
                log.warn("UserRank not found for username={}, skip.", username);
                continue;
            }

            if (!rank.getEntered()) {
                long todayAsset = totalAssetCalculator.calculate(username, account.getBalance());
                rank.setPreviousDayTotalAsset(todayAsset);
                rank.setDailyTradeAmount(0L);
                userRankRepository.save(rank);
                continue;
            }

            try {
                long todayTotalAsset = totalAssetCalculator.calculate(username, account.getBalance());

                double delta = rankScoreCalculator.calculateDailyDelta(
                        rank.getPreviousDayTotalAsset(), todayTotalAsset, rank.getDailyTradeAmount());

                rankDecisionService.applyDailyResult(rank, delta);

                rank.setPreviousDayTotalAsset(todayTotalAsset);
                rank.setDailyTradeAmount(0L);

                userRankRepository.save(rank);
                processed++;

            } catch (Exception e) {
                log.error("Daily rank batch failed for username={}", username, e);
            }
        }

        log.info("[DailyRankBatch] completed. total={}, processed={}", accounts.size(), processed);
    }
}
