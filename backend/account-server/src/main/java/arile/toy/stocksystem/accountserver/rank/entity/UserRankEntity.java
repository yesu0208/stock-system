package arile.toy.stocksystem.accountserver.rank.entity;

import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "user_ranks")
public class UserRankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRankId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private Long rp;

    /** 첫 거래 성공 전(언랭) 여부 */
    @Column(nullable = false)
    private Boolean entered;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RankLevel currentLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RankLevel highestTierReached;

    /** 전일 종가 기준 총자산 스냅샷 - 다음 날 수익률 계산의 분모/기준 */
    @Column(nullable = false)
    private Long previousDayTotalAsset;

    /** 당일 누적 거래대금(매수+매도) - 거래 발생 시마다 가산, 일일 배치 후 0으로 리셋 */
    @Column(nullable = false)
    private Long dailyTradeAmount;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    private Instant updatedDateTime;

    public static UserRankEntity of(String username, long initialAsset) {
        var entity = new UserRankEntity();
        entity.setUsername(username);
        entity.setRp(1000L);
        entity.setEntered(false);
        entity.setCurrentLevel(RankLevel.UNRANKED);
        entity.setHighestTierReached(RankLevel.UNRANKED);
        entity.setPreviousDayTotalAsset(initialAsset);
        entity.setDailyTradeAmount(0L);
        return entity;
    }

    public void addDailyTradeAmount(long amount) {
        this.dailyTradeAmount += amount;
    }

    @PrePersist
    private void prePersist() {
        this.createdDateTime = Instant.now();
        this.updatedDateTime = Instant.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedDateTime = Instant.now();
    }
}
