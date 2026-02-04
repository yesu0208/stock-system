package arile.toy.stocksystem.stockserver.trading.entity;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "auto_orders")
public class AutoOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long autoOrderId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AutoOrderType autoOrderType;

    @Column(nullable = false)
    private Integer triggerPrice;

    @Column(nullable = false)
    private Integer orderPrice;

    @Column(nullable = false)
    private Integer orderQuantity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AutoOrderStatus autoOrderStatus;

    @Column(nullable = false)
    private Instant orderTime;

    public static AutoOrderEntity of(String username, String stockCode, AutoOrderType autoOrderType,
                                     Integer triggerPrice, Integer orderPrice, Integer orderQuantity,
                                     AutoOrderStatus autoOrderStatus) {
        var autoOrderEntity = new AutoOrderEntity();
        autoOrderEntity.setUsername(username);
        autoOrderEntity.setStockCode(stockCode);
        autoOrderEntity.setAutoOrderType(autoOrderType);
        autoOrderEntity.setTriggerPrice(triggerPrice);
        autoOrderEntity.setOrderPrice(orderPrice);
        autoOrderEntity.setOrderQuantity(orderQuantity);
        autoOrderEntity.setAutoOrderStatus(autoOrderStatus);
        autoOrderEntity.setOrderTime(Instant.now());
        return autoOrderEntity;
    }

    public void changeAutoOrderStatus(AutoOrderStatus autoOrderStatus) {
        this.autoOrderStatus = autoOrderStatus;
    }
}
