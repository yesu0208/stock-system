package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankDecisionServiceTest {

    private final RankDecisionService service = new RankDecisionService();

    private static UserRankEntity rank(long rp, RankLevel current, RankLevel highest) {
        UserRankEntity rank = UserRankEntity.of("user1", 1_000_000L);
        rank.setEntered(true);
        rank.setRp(rp);
        rank.setCurrentLevel(current);
        rank.setHighestTierReached(highest);
        return rank;
    }

    @Nested
    @DisplayName("최고 등급이 골드5 미만 (강등 없음 구간)")
    class BelowGold5 {

        @Test
        @DisplayName("이익이면 RP를 반올림해 가산하고, 등급과 최고 등급을 올린다")
        void positiveDelta_promotes() {
            UserRankEntity rank = rank(1000L, RankLevel.BRONZE_5, RankLevel.BRONZE_5);

            service.applyDailyResult(rank, 175.4);

            assertThat(rank.getRp()).isEqualTo(1175L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.BRONZE_4);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.BRONZE_4);
        }

        @Test
        @DisplayName("이익이 커서 여러 등급을 한 번에 건너뛸 수 있다")
        void largePositiveDelta_skipsLevels() {
            UserRankEntity rank = rank(1000L, RankLevel.BRONZE_5, RankLevel.BRONZE_5);

            service.applyDailyResult(rank, 1000.0);   // +10% 수익

            assertThat(rank.getRp()).isEqualTo(2000L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.SILVER_5);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.SILVER_5);
        }

        @Test
        @DisplayName("손실이면 RP를 깎지 않는다")
        void negativeDelta_keepsRp() {
            UserRankEntity rank = rank(1200L, RankLevel.BRONZE_4, RankLevel.BRONZE_4);

            service.applyDailyResult(rank, -30.0);

            assertThat(rank.getRp()).isEqualTo(1200L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.BRONZE_4);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.BRONZE_4);
        }

        @Test
        @DisplayName("변화가 0이면 RP와 등급을 유지한다")
        void zeroDelta_keeps() {
            UserRankEntity rank = rank(1500L, RankLevel.BRONZE_2, RankLevel.BRONZE_2);

            service.applyDailyResult(rank, 0.0);

            assertThat(rank.getRp()).isEqualTo(1500L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.BRONZE_2);
        }

        @Test
        @DisplayName("골드5에 처음 도달하면 최고 등급도 골드5로 갱신된다")
        void reachesGold5() {
            UserRankEntity rank = rank(3240L, RankLevel.SILVER_1, RankLevel.SILVER_1);

            service.applyDailyResult(rank, 12.0);

            assertThat(rank.getRp()).isEqualTo(3252L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.GOLD_5);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.GOLD_5);
        }
    }

    @Nested
    @DisplayName("최고 등급이 골드5 이상 (강등 활성 구간)")
    class Gold5OrAbove {

        @Test
        @DisplayName("손실이면 RP를 차감하고 등급을 내리며, 최고 등급은 유지한다")
        void negativeDelta_demotes() {
            UserRankEntity rank = rank(4300L, RankLevel.GOLD_3, RankLevel.PLATINUM_5);

            service.applyDailyResult(rank, -120.0);

            assertThat(rank.getRp()).isEqualTo(4180L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.GOLD_4);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.PLATINUM_5);
        }

        @Test
        @DisplayName("골드5 아래로 떨어지면 골드5·골드5 하한 RP로 보호한다")
        void dropBelowGold5_safetyNet() {
            UserRankEntity rank = rank(3400L, RankLevel.GOLD_5, RankLevel.GOLD_4);

            service.applyDailyResult(rank, -200.0);

            assertThat(rank.getRp()).isEqualTo(RankLevel.GOLD_5.getRpLower());
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.GOLD_5);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.GOLD_4);
        }

        @Test
        @DisplayName("골드5 하한에 정확히 떨어지면 보호 없이 그대로 골드5다")
        void dropExactlyToGold5Lower() {
            UserRankEntity rank = rank(3350L, RankLevel.GOLD_5, RankLevel.GOLD_5);

            service.applyDailyResult(rank, -100.0);

            assertThat(rank.getRp()).isEqualTo(3250L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.GOLD_5);
        }

        @Test
        @DisplayName("이익으로 최고 기록을 넘으면 최고 등급을 갱신한다")
        void positiveDelta_updatesHighest() {
            UserRankEntity rank = rank(3740L, RankLevel.GOLD_5, RankLevel.GOLD_5);

            service.applyDailyResult(rank, 30.0);

            assertThat(rank.getRp()).isEqualTo(3770L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.GOLD_4);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.GOLD_4);
        }

        @Test
        @DisplayName("이익이어도 최고 기록보다 낮은 등급이면 최고 등급은 그대로다")
        void positiveDelta_belowHighest_keepsHighest() {
            UserRankEntity rank = rank(4250L, RankLevel.GOLD_3, RankLevel.PLATINUM_5);

            service.applyDailyResult(rank, 10.6);

            assertThat(rank.getRp()).isEqualTo(4261L);
            assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.GOLD_3);
            assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.PLATINUM_5);
        }
    }
}
