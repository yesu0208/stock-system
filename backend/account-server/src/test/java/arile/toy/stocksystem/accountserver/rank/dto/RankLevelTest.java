package arile.toy.stocksystem.accountserver.rank.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RankLevelTest {

    @ParameterizedTest(name = "RP {0} → {1}")
    @CsvSource({
            "-9223372036854775808, BRONZE_5",
            "999, BRONZE_5",
            "1000, BRONZE_5",
            "1149, BRONZE_5",
            "1150, BRONZE_4",
            "1749, BRONZE_1",
            "1750, SILVER_5",
            "3249, SILVER_1",
            "3250, GOLD_5",
            "5750, PLATINUM_5",
            "10750, DIAMOND_5",
            "18249, DIAMOND_1",
            "18250, MASTER",
            "9223372036854775807, MASTER"
    })
    @DisplayName("fromRp: RP가 속한 구간의 등급을 반환한다 (경계 포함)")
    void fromRp(long rp, RankLevel expected) {
        assertThat(RankLevel.fromRp(rp)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} 구간 폭 {1}")
    @CsvSource({
            "BRONZE_5, 150", "BRONZE_1, 150",
            "SILVER_5, 300", "SILVER_1, 300",
            "GOLD_5, 500", "GOLD_1, 500",
            "PLATINUM_5, 1000", "PLATINUM_1, 1000",
            "DIAMOND_5, 1500", "DIAMOND_1, 1500"
    })
    @DisplayName("티어별 구간 폭: 브론즈 150, 실버 300, 골드 500, 플래티넘 1000, 다이아 1500")
    void bandWidth(RankLevel level, long width) {
        assertThat(level.getRpUpper() - level.getRpLower() + 1).isEqualTo(width);
    }

    @Test
    @DisplayName("BRONZE_5부터 MASTER까지 등급 구간이 빈틈 없이 이어진다")
    void levelsAreContiguous() {
        RankLevel[] levels = RankLevel.values();
        for (int i = 1; i < levels.length; i++) { // UNRANKED(0) → BRONZE_5(1)부터 비교
            assertThat(levels[i].getRpLower())
                    .as("%s의 하한은 %s의 상한 + 1", levels[i], levels[i - 1])
                    .isEqualTo(levels[i - 1].getRpUpper() + 1);
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RankLevel.class, names = {"UNRANKED", "BRONZE_5", "SILVER_1"})
    @DisplayName("isGold5OrAbove: 골드5 미만과 UNRANKED는 false")
    void isGold5OrAbove_false(RankLevel level) {
        assertThat(level.isGold5OrAbove()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RankLevel.class, names = {"GOLD_5", "PLATINUM_1", "MASTER"})
    @DisplayName("isGold5OrAbove: 골드5 이상은 true")
    void isGold5OrAbove_true(RankLevel level) {
        assertThat(level.isGold5OrAbove()).isTrue();
    }

    @Test
    @DisplayName("티어와 세부 등급을 가진다 (UNRANKED, MASTER는 세부 등급 없음)")
    void tierAndSubTier() {
        assertThat(RankLevel.GOLD_3.getTier()).isEqualTo(RankLevel.Tier.GOLD);
        assertThat(RankLevel.GOLD_3.getSubTier()).isEqualTo(3);
        assertThat(RankLevel.UNRANKED.getSubTier()).isNull();
        assertThat(RankLevel.MASTER.getSubTier()).isNull();
    }
}
