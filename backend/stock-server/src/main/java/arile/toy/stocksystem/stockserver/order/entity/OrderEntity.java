package arile.toy.stocksystem.stockserver.order.entity;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeverageRatio leverageRatio;

    @Column(nullable = false)
    private Integer orderPrice;

    @Column(nullable = false)
    private Integer orderQuantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Instant orderTime;

    public static OrderEntity of(String username, String stockCode, OrderType orderType, LeverageRatio leverageRatio,
                                 Integer orderPrice, Integer orderQuantity,
                                 OrderStatus orderStatus, Integer remainingQuantity) {
        var orderEntity = new OrderEntity();
        orderEntity.setUsername(username);
        orderEntity.setStockCode(stockCode);
        orderEntity.setOrderType(orderType);
        orderEntity.setLeverageRatio(leverageRatio);
        orderEntity.setOrderPrice(orderPrice);
        orderEntity.setOrderQuantity(orderQuantity);
        orderEntity.setOrderStatus(orderStatus);
        orderEntity.setRemainingQuantity(remainingQuantity);
        orderEntity.setOrderTime(Instant.now());
        return orderEntity;
    }

    public void changeOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
}
