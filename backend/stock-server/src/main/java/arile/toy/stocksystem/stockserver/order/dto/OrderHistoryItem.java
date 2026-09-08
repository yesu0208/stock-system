package arile.toy.stocksystem.stockserver.order.dto;

import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;

import java.time.Instant;

public record OrderHistoryItem(
        Long orderId,
        String stockCode,
        OrderType orderType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer orderQuantity,
        Integer remainingQuantity,
        OrderStatus orderStatus,
        Instant orderTime
) {
    public static OrderHistoryItem fromEntity(OrderEntity entity) {
        return new OrderHistoryItem(
                entity.getOrderId(), entity.getStockCode(), entity.getOrderType(), entity.getLeverageRatio(),
                entity.getOrderPrice(), entity.getOrderQuantity(), entity.getRemainingQuantity(),
                entity.getOrderStatus(), entity.getOrderTime()
        );
    }
}
