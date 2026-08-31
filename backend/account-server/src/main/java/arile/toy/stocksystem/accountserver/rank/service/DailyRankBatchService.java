package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.RankHistoryRepository;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.useraccount.entity.UserAccountEntity;
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
public class DailyRankBatchService {

    private final UserAccountRepository userAccountRepository;
    private final UserRankRepository userRankRepository;
    private final RankHistoryRepository rankHistoryRepository;
    private final TotalAssetCalculator totalAssetCalculator;
    private final RankScoreCalculator rankScoreCalculator;
    private final RankDecisionService rankDecisionService;

    @Transactional
    public void runDailyBatch() {

        LocalDate today = LocalDate.now();

        List<UserAccountEntity> accounts = userAccountRepository.findAll();
        int processed = 0;

        for (UserAccountEntity account : accounts) {

            String username = account.getUsername();

            UserRankEntity rank = userRankRepository.findByUsername(username).orElse(null);
            if (rank == null) {
                log.warn("UserRank not found for username={}, skip.", username);
                continue;
            }

            if (rankHistoryRepository.existsByUsernameAndRecordDate(username, today)) {
                log.warn("Rank history already recorded for username={}, date={}. Skip to avoid duplicate.",
                        username, today);
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

                long rpBefore = rank.getRp();

                rankDecisionService.applyDailyResult(rank, delta);

                long rpChange = rank.getRp() - rpBefore;

                rank.setPreviousDayTotalAsset(todayTotalAsset);
                rank.setDailyTradeAmount(0L);

                userRankRepository.save(rank);

                rankHistoryRepository.save(
                        RankHistoryEntity.of(username, today, rank.getCurrentLevel(), rank.getRp(), rpChange)
                );

                processed++;

            } catch (Exception e) {
                log.error("Daily rank batch failed for username={}", username, e);
            }
        }

        log.info("[DailyRankBatch] completed. total={}, processed={}", accounts.size(), processed);
    }
}
