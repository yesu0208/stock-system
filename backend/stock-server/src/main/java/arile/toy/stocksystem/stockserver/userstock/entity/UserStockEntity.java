package arile.toy.stocksystem.stockserver.userstock.entity;

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

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    private Instant updatedDateTime;

    public static UserStockEntity of(String username, String stockCode,
                                     Long amount, Integer quantity) {
        var userStockEntity = new UserStockEntity();
        userStockEntity.setUsername(username);
        userStockEntity.setStockCode(stockCode);
        userStockEntity.setAmount(amount);
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
