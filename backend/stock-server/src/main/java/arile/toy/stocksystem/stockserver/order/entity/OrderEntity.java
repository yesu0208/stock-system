package arile.toy.stocksystem.stockserver.order.entity;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderExecutionType orderExecutionType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderOrigin origin;

    // autoOrderId / otocoId / trailingStopId / MANUAL이면 null
    private Long originId;

    private Long remainingReservedFee;

    private Long remainingReservedMargin;

    public static OrderEntity of(String username, String stockCode, OrderType orderType, LeverageRatio leverageRatio,
                                 Integer orderPrice, Integer orderQuantity,
                                 OrderStatus orderStatus, Integer remainingQuantity,
                                 OrderExecutionType orderExecutionType,
                                 OrderOrigin origin, Long originId,
                                 Long remainingReservedFee,
                                 Long remainingReservedMargin) {
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
        orderEntity.setOrderExecutionType(orderExecutionType);
        orderEntity.setOrigin(origin);
        orderEntity.setOriginId(originId);
        orderEntity.setRemainingReservedFee(remainingReservedFee);
        orderEntity.setRemainingReservedMargin(remainingReservedMargin);
        return orderEntity;
    }

    public void changeOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public long consumeReservedFee(int executedQuantity) {
        if (remainingQuantity == null || remainingQuantity <= 0 || remainingReservedFee == null) {
            return 0L;
        }
        long feeForThisFill = remainingQuantity.equals(executedQuantity)
                ? remainingReservedFee
                : Math.round((double) remainingReservedFee * executedQuantity / remainingQuantity);

        remainingReservedFee -= feeForThisFill;
        return feeForThisFill;
    }

    public long consumeReservedMargin(int executedQuantity) {
        if (remainingQuantity == null || remainingQuantity <= 0 || remainingReservedMargin == null) {
            return 0L;
        }
        long marginForThisFill = remainingQuantity.equals(executedQuantity)
                ? remainingReservedMargin
                : Math.round((double) remainingReservedMargin * executedQuantity / remainingQuantity);

        remainingReservedMargin -= marginForThisFill;
        return marginForThisFill;
    }
}
