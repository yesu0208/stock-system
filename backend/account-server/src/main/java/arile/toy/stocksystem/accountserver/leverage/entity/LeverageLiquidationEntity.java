package arile.toy.stocksystem.accountserver.leverage.entity;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "leverage_liquidations")
public class LeverageLiquidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long liquidationId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeverageRatio leverageRatio;

    @Column(nullable = false)
    private Integer liquidatedQuantity;

    @Column(nullable = false)
    private Long settlementPrice;

    @Column(nullable = false)
    private Long proceeds; // 청산 대금 (수량 × 정산가)

    @Column(nullable = false)
    private Long repaidLoanAmount; // 상환된 대출금 (전액)

    @Column(nullable = false)
    private Long shortfall; // 부족분 (0 이상, 대금이 대출금보다 적을 때만 양수)

    @Column(nullable = false)
    private Instant liquidatedAt;

    public static LeverageLiquidationEntity of(String username, String stockCode, LeverageRatio leverageRatio,
                                               Integer liquidatedQuantity, Long settlementPrice,
                                               Long proceeds, Long repaidLoanAmount, Long shortfall) {
        var entity = new LeverageLiquidationEntity();
        entity.setUsername(username);
        entity.setStockCode(stockCode);
        entity.setLeverageRatio(leverageRatio);
        entity.setLiquidatedQuantity(liquidatedQuantity);
        entity.setSettlementPrice(settlementPrice);
        entity.setProceeds(proceeds);
        entity.setRepaidLoanAmount(repaidLoanAmount);
        entity.setShortfall(shortfall);
        return entity;
    }

    @PrePersist
    private void prePersist() {
        this.liquidatedAt = Instant.now();
    }
}
