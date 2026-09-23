package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.dailyreturn.service.DailyReturnRecorder;
import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.RankHistoryRepository;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 사용자 1명의 일일 랭크·수익률 정산 실행기.
 * DailyRankBatchService의 배치 루프에서 호출되며, 사용자마다 개별 트랜잭션으로 실행
 * (배치 전체를 하나의 트랜잭션으로 묶으면 한 사용자의 DB 예외로 트랜잭션 전체가 rollback-only가 되어
 *  이미 처리된 다른 사용자의 랭크·수익률 기록까지 롤백되므로 사용자 단위로 분리)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyRankExecutor {

    private final UserRankRepository userRankRepository;
    private final RankHistoryRepository rankHistoryRepository;
    private final TotalAssetCalculator totalAssetCalculator;
    private final RankScoreCalculator rankScoreCalculator;
    private final RankDecisionService rankDecisionService;
    private final DailyReturnRecorder dailyReturnRecorder;

    public enum DailyRankOutcome {
        /** 랭크 참여자 — RP·등급 갱신 및 랭크 이력 기록 */
        RANKED,
        /** 언랭 — 수익률 기록과 전일 총자산 스냅샷만 갱신 */
        UNRANKED_SNAPSHOT,
        /** 랭크 정보 없음 또는 오늘 이미 처리됨 */
        SKIPPED
    }

    @Transactional
    public DailyRankOutcome processOneUser(String username, long cashBalance, LocalDate today) {

        // 체결 반영(TradeExecutionApplyService)과 같은 락을 잡아 당일 거래대금 누적분이 덮어써지지 않도록 함
        UserRankEntity rank = userRankRepository.findByUsernameForUpdate(username).orElse(null);
        if (rank == null) {
            log.warn("UserRank not found for username={}, skip.", username);
            return DailyRankOutcome.SKIPPED;
        }

        if (rankHistoryRepository.existsByUsernameAndRecordDate(username, today)) {
            log.warn("Rank history already recorded for username={}, date={}. Skip to avoid duplicate.",
                    username, today);
            return DailyRankOutcome.SKIPPED;
        }

        if (!rank.getEntered()) {
            long todayAsset = totalAssetCalculator.calculate(username, cashBalance);

            dailyReturnRecorder.record(username, today,
                    rank.getPreviousDayTotalAsset(), todayAsset, rank.getDailyTradeAmount());

            rank.setPreviousDayTotalAsset(todayAsset);
            rank.setDailyTradeAmount(0L);
            userRankRepository.save(rank);
            return DailyRankOutcome.UNRANKED_SNAPSHOT;
        }

        long todayTotalAsset = totalAssetCalculator.calculate(username, cashBalance);

        double delta = rankScoreCalculator.calculateDailyDelta(
                rank.getPreviousDayTotalAsset(), todayTotalAsset, rank.getDailyTradeAmount());

        dailyReturnRecorder.record(username, today,
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

        return DailyRankOutcome.RANKED;
    }
}
