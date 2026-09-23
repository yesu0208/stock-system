package arile.toy.stocksystem.accountserver.rank.dto;

import arile.toy.stocksystem.accountserver.rank.entity.RankHistoryEntity;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RankResponseTest {

    private static UserRankEntity rank(long rp, RankLevel current, RankLevel highest) {
        UserRankEntity rank = UserRankEntity.of("user1", 1_000_000L);
        rank.setRp(rp);
        rank.setCurrentLevel(current);
        rank.setHighestTierReached(highest);
        return rank;
    }

    @Test
    @DisplayName("RankResponse: 현재 등급의 하한 RP와 한 단계 위 등급의 하한 RP를 담는다")
    void rankResponse() {
        RankResponse response = RankResponse.fromEntity(rank(3800L, RankLevel.GOLD_4, RankLevel.PLATINUM_5));

        assertThat(response.username()).isEqualTo("user1");
        assertThat(response.tier()).isEqualTo("GOLD");
        assertThat(response.subTier()).isEqualTo(4);
        assertThat(response.rp()).isEqualTo(3800L);
        assertThat(response.highestTierReached()).isEqualTo("PLATINUM_5");
        assertThat(response.currentRankMinRp()).isEqualTo(3750L);
        assertThat(response.nextRankMinRp()).isEqualTo(4250L);
    }

    @Test
    @DisplayName("RankResponse: 언랭 상태면 다음 등급은 BRONZE_5다")
    void rankResponse_unranked() {
        RankResponse response = RankResponse.fromEntity(rank(1000L, RankLevel.UNRANKED, RankLevel.UNRANKED));

        assertThat(response.tier()).isEqualTo("UNRANKED");
        assertThat(response.subTier()).isNull();
        assertThat(response.nextRankMinRp()).isEqualTo(RankLevel.BRONZE_5.getRpLower());
    }

    @Test
    @DisplayName("RankResponse: 최고 등급(MASTER)이면 다음 등급 RP는 null이다")
    void rankResponse_master() {
        RankResponse response = RankResponse.fromEntity(rank(20_000L, RankLevel.MASTER, RankLevel.MASTER));

        assertThat(response.tier()).isEqualTo("MASTER");
        assertThat(response.subTier()).isNull();
        assertThat(response.currentRankMinRp()).isEqualTo(18_250L);
        assertThat(response.nextRankMinRp()).isNull();
    }

    @Test
    @DisplayName("RankHistoryItem: 이력 엔티티의 날짜·티어·세부 등급·RP·변화량을 담는다")
    void rankHistoryItem() {
        LocalDate date = LocalDate.of(2026, 9, 21);
        RankHistoryEntity entity = RankHistoryEntity.of("user1", date, RankLevel.SILVER_2, 2700L, 150L);

        RankHistoryItem item = RankHistoryItem.fromEntity(entity);

        assertThat(item).isEqualTo(new RankHistoryItem(date, "SILVER", 2, 2700L, 150L));
    }
}
