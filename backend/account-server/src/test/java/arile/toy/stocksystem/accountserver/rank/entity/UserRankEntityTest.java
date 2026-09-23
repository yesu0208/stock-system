package arile.toy.stocksystem.accountserver.rank.entity;

import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserRankEntityTest {

    @Test
    @DisplayName("of: 언랭 상태, BRONZE_5 하한 RP, 초기 자산을 전일 총자산으로, 거래대금 0으로 시작한다")
    void of() {
        UserRankEntity rank = UserRankEntity.of("user1", 1_000_000_000L);

        assertThat(rank.getUsername()).isEqualTo("user1");
        assertThat(rank.getRp()).isEqualTo(RankLevel.BRONZE_5.getRpLower());
        assertThat(rank.getEntered()).isFalse();
        assertThat(rank.getCurrentLevel()).isEqualTo(RankLevel.UNRANKED);
        assertThat(rank.getHighestTierReached()).isEqualTo(RankLevel.UNRANKED);
        assertThat(rank.getPreviousDayTotalAsset()).isEqualTo(1_000_000_000L);
        assertThat(rank.getDailyTradeAmount()).isZero();
    }

    @Test
    @DisplayName("addDailyTradeAmount: 당일 거래대금을 누적한다")
    void addDailyTradeAmount() {
        UserRankEntity rank = UserRankEntity.of("user1", 1_000_000L);

        rank.addDailyTradeAmount(690_000L);
        rank.addDailyTradeAmount(284_000L);

        assertThat(rank.getDailyTradeAmount()).isEqualTo(974_000L);
    }

    @Test
    @DisplayName("prePersist: 생성·수정 시각을 채운다")
    void prePersist() {
        UserRankEntity rank = UserRankEntity.of("user1", 1_000_000L);

        ReflectionTestUtils.invokeMethod(rank, "prePersist");

        assertThat(rank.getCreatedDateTime()).isNotNull();
        assertThat(rank.getUpdatedDateTime()).isNotNull();
    }

    @Test
    @DisplayName("preUpdate: 수정 시각만 갱신한다")
    void preUpdate() {
        UserRankEntity rank = UserRankEntity.of("user1", 1_000_000L);
        Instant created = Instant.parse("2026-09-01T00:00:00Z");
        rank.setCreatedDateTime(created);
        rank.setUpdatedDateTime(created);

        ReflectionTestUtils.invokeMethod(rank, "preUpdate");

        assertThat(rank.getCreatedDateTime()).isEqualTo(created);
        assertThat(rank.getUpdatedDateTime()).isAfter(created);
    }
}
