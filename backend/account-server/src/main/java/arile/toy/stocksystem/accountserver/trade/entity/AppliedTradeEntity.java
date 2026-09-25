package arile.toy.stocksystem.accountserver.trade.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * account-server에 반영 완료된 체결 기록.
 * stock-server의 체결 outbox는 최소 1회 전달(중복 발행 가능)이므로,
 * 같은 체결(stockCode + tradeId)이 두 번 정산되지 않도록 반영 여부를 기록.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "applied_trades",
        uniqueConstraints = @UniqueConstraint(name = "uk_applied_trade", columnNames = {"stockCode", "tradeId"})
)
public class AppliedTradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    private Long tradeId;

    @Column(nullable = false)
    private Instant appliedAt;

    public static AppliedTradeEntity of(String stockCode, Long tradeId) {
        AppliedTradeEntity entity = new AppliedTradeEntity();
        entity.stockCode = stockCode;
        entity.tradeId = tradeId;
        entity.appliedAt = Instant.now();
        return entity;
    }
}
