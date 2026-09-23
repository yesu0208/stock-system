package arile.toy.stocksystem.accountserver.rank.dto;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum RankLevel {
    UNRANKED(Tier.UNRANKED, null, Long.MIN_VALUE, 999),
    BRONZE_5(Tier.BRONZE, 5, 1000, 1149),
    BRONZE_4(Tier.BRONZE, 4, 1150, 1299),
    BRONZE_3(Tier.BRONZE, 3, 1300, 1449),
    BRONZE_2(Tier.BRONZE, 2, 1450, 1599),
    BRONZE_1(Tier.BRONZE, 1, 1600, 1749),
    SILVER_5(Tier.SILVER, 5, 1750, 2049),
    SILVER_4(Tier.SILVER, 4, 2050, 2349),
    SILVER_3(Tier.SILVER, 3, 2350, 2649),
    SILVER_2(Tier.SILVER, 2, 2650, 2949),
    SILVER_1(Tier.SILVER, 1, 2950, 3249),
    GOLD_5(Tier.GOLD, 5, 3250, 3749),
    GOLD_4(Tier.GOLD, 4, 3750, 4249),
    GOLD_3(Tier.GOLD, 3, 4250, 4749),
    GOLD_2(Tier.GOLD, 2, 4750, 5249),
    GOLD_1(Tier.GOLD, 1, 5250, 5749),
    PLATINUM_5(Tier.PLATINUM, 5, 5750, 6749),
    PLATINUM_4(Tier.PLATINUM, 4, 6750, 7749),
    PLATINUM_3(Tier.PLATINUM, 3, 7750, 8749),
    PLATINUM_2(Tier.PLATINUM, 2, 8750, 9749),
    PLATINUM_1(Tier.PLATINUM, 1, 9750, 10749),
    DIAMOND_5(Tier.DIAMOND, 5, 10750, 12249),
    DIAMOND_4(Tier.DIAMOND, 4, 12250, 13749),
    DIAMOND_3(Tier.DIAMOND, 3, 13750, 15249),
    DIAMOND_2(Tier.DIAMOND, 2, 15250, 16749),
    DIAMOND_1(Tier.DIAMOND, 1, 16750, 18249),
    MASTER(Tier.MASTER, null, 18250, Long.MAX_VALUE);

    private final Tier tier;
    private final Integer subTier;
    private final long rpLower;
    private final long rpUpper;

    RankLevel(Tier tier, Integer subTier, long rpLower, long rpUpper) {
        this.tier = tier;
        this.subTier = subTier;
        this.rpLower = rpLower;
        this.rpUpper = rpUpper;
    }

    /** RP 값으로 브론즈5~마스터 범위 내 이론상 등급을 계산한다. UNRANKED는 별도 상태이므로 제외. */
    public static RankLevel fromRp(long rp) {
        // 브론즈5 하한 미만은 최하위 등급으로 간주 (기존에는 어느 구간에도 속하지 않아 MASTER가 반환되었음)
        if (rp < BRONZE_5.rpLower) {
            return BRONZE_5;
        }
        // 구간이 오름차순으로 빈틈없이 이어지므로, 상한이 rp 이상인 첫 등급이 해당 등급
        return Arrays.stream(values())
                .filter(level -> level != UNRANKED)
                .filter(level -> rp <= level.rpUpper)
                .findFirst()
                .orElse(MASTER);
    }

    /** 골드5 이상(강등 활성 구간 진입 이력) 여부 - highestTierReached 판단에 사용 */
    public boolean isGold5OrAbove() {
        return this != UNRANKED && this.ordinal() >= GOLD_5.ordinal();
    }

    public enum Tier {
        UNRANKED, BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, MASTER
    }
}
