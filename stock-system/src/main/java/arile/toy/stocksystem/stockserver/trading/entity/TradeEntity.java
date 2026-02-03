package arile.toy.stocksystem.stockserver.trading.entity;

import arile.toy.stocksystem.stockserver.trading.dto.trade.TradeType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "trades")
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    @Column(nullable = false)
    private Integer tradePrice;

    @Column(nullable = false)
    private Integer tradeQuantity;

    @Column(nullable = false)
    private Instant executedAt;

    public static TradeEntity of(Long orderId, String username, String stockCode,
                                 TradeType tradeType, Integer orderPrice, Integer orderQuantity) {
        var tradeEntity = new TradeEntity();
        tradeEntity.setOrderId(orderId);
        tradeEntity.setUsername(username);
        tradeEntity.setStockCode(stockCode);
        tradeEntity.setTradeType(tradeType);
        tradeEntity.setTradePrice(orderPrice);
        tradeEntity.setTradeQuantity(orderQuantity);
        return tradeEntity;
    }
    
    @PrePersist
    private void prePersist() {
        this.executedAt = Instant.now();
    }
}
