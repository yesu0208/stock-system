package arile.toy.stocksystem.stockserver.cancel.event;

import arile.toy.stocksystem.stockserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;

public record CancelResponseEvent(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        boolean success,
        CancelErrorCode errorCode
) {
    public static CancelResponseEvent of(OrderEntity order, boolean success, CancelErrorCode errorCode) {
        return new CancelResponseEvent(
                order.getOrderId(),
                order.getUsername(),
                order.getStockCode(),
                order.getOrderType(),
                order.getOrderPrice(),
                order.getOrderQuantity(),
                success,
                errorCode
        );
    }
}
