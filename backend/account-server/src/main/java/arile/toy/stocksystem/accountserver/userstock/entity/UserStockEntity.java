package arile.toy.stocksystem.accountserver.userstock.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "user_stocks")
public class UserStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userStockId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    /** 순수 매입금액 (체결가 × 수량 누적, 수수료 제외) */
    @Column(nullable = false)
    private Long amount;

    /** 매입원금액 — amount + 매수 시 실제 부과된 위탁수수료 누적. 손익분기가/평가손익 계산의 기준. */
    @Column(nullable = false)
    private Long costAmount;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    private Instant updatedDateTime;

    public static UserStockEntity of(String username, String stockCode,
                                     Long amount, Long costAmount, Integer quantity) {
        var userStockEntity = new UserStockEntity();
        userStockEntity.setUsername(username);
        userStockEntity.setStockCode(stockCode);
        userStockEntity.setAmount(amount);
        userStockEntity.setCostAmount(costAmount);
        userStockEntity.setQuantity(quantity);
        return userStockEntity;
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
