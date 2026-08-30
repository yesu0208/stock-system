package arile.toy.stocksystem.accountserver.rank.dto;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum RankLevel {
    UNRANKED(Tier.UNRANKED, null, Long.MIN_VALUE, 999, false),
    BRONZE_5(Tier.BRONZE, 5, 1000, 1019, false),
    BRONZE_4(Tier.BRONZE, 4, 1020, 1044, false),
    BRONZE_3(Tier.BRONZE, 3, 1045, 1074, false),
    BRONZE_2(Tier.BRONZE, 2, 1075, 1109, false),
    BRONZE_1(Tier.BRONZE, 1, 1110, 1149, false),
    SILVER_5(Tier.SILVER, 5, 1150, 1199, false),
    SILVER_4(Tier.SILVER, 4, 1200, 1259, false),
    SILVER_3(Tier.SILVER, 3, 1260, 1329, false),
    SILVER_2(Tier.SILVER, 2, 1330, 1409, false),
    SILVER_1(Tier.SILVER, 1, 1410, 1499, false),
    GOLD_5(Tier.GOLD, 5, 1500, 1819, false),
    GOLD_4(Tier.GOLD, 4, 1820, 2199, true),
    GOLD_3(Tier.GOLD, 3, 2200, 2649, true),
    GOLD_2(Tier.GOLD, 2, 2650, 3179, true),
    GOLD_1(Tier.GOLD, 1, 3180, 3799, true),
    PLATINUM_5(Tier.PLATINUM, 5, 3800, 4519, true),
    PLATINUM_4(Tier.PLATINUM, 4, 4520, 5359, true),
    PLATINUM_3(Tier.PLATINUM, 3, 5360, 6329, true),
    PLATINUM_2(Tier.PLATINUM, 2, 6330, 7449, true),
    PLATINUM_1(Tier.PLATINUM, 1, 7450, 8739, true),
    DIAMOND_5(Tier.DIAMOND, 5, 8740, 10229, true),
    DIAMOND_4(Tier.DIAMOND, 4, 10230, 11949, true),
    DIAMOND_3(Tier.DIAMOND, 3, 11950, 13929, true),
    DIAMOND_2(Tier.DIAMOND, 2, 13930, 16209, true),
    DIAMOND_1(Tier.DIAMOND, 1, 16210, 18829, true),
    MASTER(Tier.MASTER, null, 18830, Long.MAX_VALUE, true);

    private final Tier tier;
    private final Integer subTier;
    private final long rpLower;
    private final long rpUpper;
    private final boolean demotable;

    RankLevel(Tier tier, Integer subTier, long rpLower, long rpUpper, boolean demotable) {
        this.tier = tier;
        this.subTier = subTier;
        this.rpLower = rpLower;
        this.rpUpper = rpUpper;
        this.demotable = demotable;
    }

    /** RP 값으로 브론즈5~마스터 범위 내 이론상 등급을 계산한다. UNRANKED는 별도 상태이므로 제외. */
    public static RankLevel fromRp(long rp) {
        return Arrays.stream(values())
                .filter(level -> level != UNRANKED)
                .filter(level -> rp >= level.rpLower && rp <= level.rpUpper)
                .findFirst()
                .orElse(MASTER);
    }

    /** 골드5 이상(강등 활성 구간 진입 이력) 여부 - highestTierReached 판단에 사용 */
    public boolean isGold5OrAbove() {
        return this != UNRANKED && this.ordinal() >= GOLD_5.ordinal();
    }

    /** 강등 활성 구간(골드4 이상)에 도달한 적 있는지 */
    public boolean isDemotionActivated() {
        return this != UNRANKED && this.ordinal() >= GOLD_4.ordinal();
    }

    public enum Tier {
        UNRANKED, BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, MASTER
    }
}
