package arile.toy.stocksystem.stockserver.trading.dto.order;

import arile.toy.stocksystem.stockserver.trading.entity.OrderEntity;

public record UpdateOrderStatusResult(
        OrderEntity orderEntity,
        OrderStatus previousStatus
) {
    public static UpdateOrderStatusResult of(OrderEntity orderEntity, OrderStatus previousStatus) {
        return new UpdateOrderStatusResult(orderEntity, previousStatus);
    }
}
