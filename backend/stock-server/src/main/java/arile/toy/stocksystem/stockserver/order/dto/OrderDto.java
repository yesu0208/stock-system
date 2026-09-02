package arile.toy.stocksystem.stockserver.order.dto;

import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;

import java.time.Instant;

public record OrderDto(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer orderQuantity,
        Integer remainingQuantity,
        OrderStatus orderStatus,
        Instant orderTime
) {
    public static OrderDto fromEntity(OrderEntity orderEntity) {
        return new OrderDto(orderEntity.getOrderId(),
                orderEntity.getUsername(),
                orderEntity.getStockCode(),
                orderEntity.getOrderType(),
                orderEntity.getLeverageRatio(),
                orderEntity.getOrderPrice(),
                orderEntity.getOrderQuantity(),
                orderEntity.getRemainingQuantity(),
                orderEntity.getOrderStatus(),
                orderEntity.getOrderTime());
    }
}
