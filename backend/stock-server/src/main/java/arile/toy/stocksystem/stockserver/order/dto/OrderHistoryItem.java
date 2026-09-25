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
        Instant orderTime,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice,
        OrderExecutionType orderExecutionType,
        OrderOrigin origin,
        Long originId
) {
    public static OrderHistoryItem fromEntity(OrderEntity entity) {
        LeverageMetrics metrics = LeverageMetrics.of(
                entity.getLeverageRatio(), entity.getOrderPrice(), entity.getOrderQuantity());

        return new OrderHistoryItem(
                entity.getOrderId(), entity.getStockCode(), entity.getOrderType(), entity.getLeverageRatio(),
                entity.getOrderPrice(), entity.getOrderQuantity(), entity.getRemainingQuantity(),
                entity.getOrderStatus(), entity.getOrderTime(),
                metrics.notionalValue(), metrics.initialMargin(),
                metrics.maintenanceMarginRate(), metrics.liquidationPrice(),
                entity.getOrderExecutionType(),
                entity.getOrigin(),
                entity.getOriginId()
        );
    }
}
