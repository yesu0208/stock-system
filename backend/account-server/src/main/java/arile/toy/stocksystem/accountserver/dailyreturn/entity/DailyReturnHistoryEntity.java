package arile.toy.stocksystem.accountserver.dailyreturn.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "daily_return_histories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "recordDate"}))
public class DailyReturnHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dailyReturnHistoryId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDate recordDate;

    /** 전일 종가 기준 총자산 */
    @Column(nullable = false)
    private Long previousDayTotalAsset;

    /** 당일 총자산(현금+현물+레버리지 순자산) */
    @Column(nullable = false)
    private Long totalAsset;

    /** 당일 손익금액 = totalAsset - previousDayTotalAsset */
    @Column(nullable = false)
    private Long dailyProfitAmount;

    /** 당일 수익률(%) */
    @Column(nullable = false)
    private Double dailyProfitRate;

    /** 당일 누적 거래대금 */
    @Column(nullable = false)
    private Long dailyTradeAmount;

    /** 초기자본 대비 누적 손익금액 */
    @Column(nullable = false)
    private Long cumulativeProfitAmount;

    /** 초기자본 대비 누적 수익률(%) */
    @Column(nullable = false)
    private Double cumulativeProfitRate;

    public static DailyReturnHistoryEntity of(
            String username, LocalDate recordDate, Long previousDayTotalAsset, Long totalAsset,
            Long dailyProfitAmount, Double dailyProfitRate, Long dailyTradeAmount,
            Long cumulativeProfitAmount, Double cumulativeProfitRate) {
        var entity = new DailyReturnHistoryEntity();
        entity.setUsername(username);
        entity.setRecordDate(recordDate);
        entity.setPreviousDayTotalAsset(previousDayTotalAsset);
        entity.setTotalAsset(totalAsset);
        entity.setDailyProfitAmount(dailyProfitAmount);
        entity.setDailyProfitRate(dailyProfitRate);
        entity.setDailyTradeAmount(dailyTradeAmount);
        entity.setCumulativeProfitAmount(cumulativeProfitAmount);
        entity.setCumulativeProfitRate(cumulativeProfitRate);
        return entity;
    }
}
