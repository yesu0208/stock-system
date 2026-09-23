package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.dailyreturn.service.DailyReturnRecorder;
import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import arile.toy.stocksystem.accountserver.rank.repository.RankHistoryRepository;
import arile.toy.stocksystem.accountserver.rank.repository.UserRankRepository;
import arile.toy.stocksystem.accountserver.rank.service.DailyRankExecutor.DailyRankOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DailyRankExecutorTest {

    private static final String USERNAME = "user1";
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final long CASH = 800_000L;

    @Mock private UserRankRepository userRankRepository;
    @Mock private RankHistoryRepository rankHistoryRepository;
    @Mock private TotalAssetCalculator totalAssetCalculator;
    @Mock private RankScoreCalculator rankScoreCalculator;
    @Mock private DailyReturnRecorder dailyReturnRecorder;

    private DailyRankExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DailyRankExecutor(userRankRepository, rankHistoryRepository, totalAssetCalculator,
                rankScoreCalculator, new RankDecisionService(), dailyReturnRecorder);
    }

    /** 전일 총자산 1,000,000, 당일 거래대금 50,000,000 */
    private UserRankEntity givenRank(boolean entered) {
        UserRankEntity rank = UserRankEntity.of(USERNAME, 1_000_000L);
        rank.addDailyTradeAmount(50_000_000L);
        if (entered) {
            rank.setEntered(true);
            rank.setCurrentLevel(RankLevel.BRONZE_5);
            rank.setHighestTierReached(RankLevel.BRONZE_5);
        }
        given(userRankRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.of(rank));
        return rank;
    }

    private void givenNotYetRecorded() {
        given(rankHistoryRepository.existsByUsernameAndRecordDate(USERNAME, TODAY)).willReturn(false);
    }

    @Test
    @DisplayName("랭크 참여자: 점수만큼 RP·등급을 갱신하고, 수익률·랭크 이력을 기록한 뒤 전일 총자산과 거래대금을 초기화한다")
    void ranked() {
        UserRankEntity rank = givenRank(true);
        givenNotYetRecorded();
        given(totalAssetCalculator.calculate(USERNAME, CASH)).willReturn(1_050_000L);
        given(rankScoreCalculator.calculateDailyDelta(1_000_000L, 1_050_000L, 50_000_000L)).willReturn(505.0);

        DailyRankOutcome outcome = executor.processOneUser(USERNAME, CASH, TODAY);

        assertThat(outcome).isEqualTo(DailyRankOutcome.RANKED);

        // 1000 + 505 = 1505 → BRONZE_2 (1450~1599)
        assertThat(rank.getRp()).isEqualTo(1505L);
        assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.BRONZE_2);
        assertThat(rank.getPreviousDayTotalAsset()).isEqualTo(1_050_000L);
        assertThat(rank.getDailyTradeAmount()).isZero();

        verify(dailyReturnRecorder).record(USERNAME, TODAY, 1_000_000L, 1_050_000L, 50_000_000L);
        verify(userRankRepository).save(rank);
        verify(rankHistoryRepository).save(
                RankHistoryEntity.of(USERNAME, TODAY, RankLevel.BRONZE_2, 1505L, 505L));
    }

    @Test
    @DisplayName("언랭: 점수·등급은 건드리지 않고 수익률 기록과 전일 총자산 스냅샷만 갱신한다")
    void unranked() {
        UserRankEntity rank = givenRank(false);
        givenNotYetRecorded();
        given(totalAssetCalculator.calculate(USERNAME, CASH)).willReturn(990_000L);

        DailyRankOutcome outcome = executor.processOneUser(USERNAME, CASH, TODAY);

        assertThat(outcome).isEqualTo(DailyRankOutcome.UNRANKED_SNAPSHOT);
        assertThat(rank.getRp()).isEqualTo(RankLevel.BRONZE_5.getRpLower());
        assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.UNRANKED);
        assertThat(rank.getPreviousDayTotalAsset()).isEqualTo(990_000L);
        assertThat(rank.getDailyTradeAmount()).isZero();

        verify(dailyReturnRecorder).record(USERNAME, TODAY, 1_000_000L, 990_000L, 50_000_000L);
        verify(userRankRepository).save(rank);
        verifyNoInteractions(rankScoreCalculator);
        verify(rankHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("랭크 정보가 없으면 건너뛴다")
    void whenRankNotFound_skips() {
        given(userRankRepository.findByUsernameForUpdate(USERNAME)).willReturn(Optional.empty());

        assertThat(executor.processOneUser(USERNAME, CASH, TODAY)).isEqualTo(DailyRankOutcome.SKIPPED);

        verifyNoInteractions(rankHistoryRepository, totalAssetCalculator, dailyReturnRecorder);
    }

    @Test
    @DisplayName("오늘 랭크 이력이 이미 있으면 중복 처리하지 않는다")
    void whenAlreadyRecorded_skips() {
        UserRankEntity rank = givenRank(true);
        given(rankHistoryRepository.existsByUsernameAndRecordDate(USERNAME, TODAY)).willReturn(true);

        assertThat(executor.processOneUser(USERNAME, CASH, TODAY)).isEqualTo(DailyRankOutcome.SKIPPED);

        assertThat(rank.getRp()).isEqualTo(RankLevel.BRONZE_5.getRpLower());
        verifyNoInteractions(totalAssetCalculator, dailyReturnRecorder);
        verify(userRankRepository, never()).save(any());
    }

    @Test
    @DisplayName("총자산 계산 중 예외가 나면 저장 없이 전파한다 (트랜잭션 롤백)")
    void whenCalculationFails_throws() {
        givenRank(true);
        givenNotYetRecorded();
        given(totalAssetCalculator.calculate(USERNAME, CASH)).willThrow(new IllegalStateException("redis down"));

        assertThatThrownBy(() -> executor.processOneUser(USERNAME, CASH, TODAY))
                .isInstanceOf(IllegalStateException.class);

        verify(userRankRepository, never()).save(any());
        verify(rankHistoryRepository, never()).save(any());
    }
}
