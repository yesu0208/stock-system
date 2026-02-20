package arile.toy.stocksystem.stockserver.order.dto;

import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;

public record UpdateOrderStatusResult(
        OrderEntity orderEntity,
        OrderStatus previousStatus
) {
    public static UpdateOrderStatusResult of(OrderEntity orderEntity, OrderStatus previousStatus) {
        return new UpdateOrderStatusResult(orderEntity, previousStatus);
    }
}
