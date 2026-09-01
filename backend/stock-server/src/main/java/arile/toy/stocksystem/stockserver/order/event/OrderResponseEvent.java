package arile.toy.stocksystem.stockserver.order.event;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderErrorCode;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;

import java.time.Instant;

public record OrderResponseEvent(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        boolean success,
        OrderErrorCode errorCode
) {
    public static OrderResponseEvent fromOrderResponseMessage(StockServerOrderResponseMessage orderResponseMessage,
                                                              boolean success, OrderErrorCode errorCode) {
        return new OrderResponseEvent(orderResponseMessage.orderId(), orderResponseMessage.username(),
                orderResponseMessage.stockCode(), orderResponseMessage.orderType(), orderResponseMessage.leverageRatio(),
                orderResponseMessage.orderPrice(), orderResponseMessage.orderQuantity(),
                orderResponseMessage.orderTime(), success, errorCode);
    }

    public static OrderResponseEvent of(Long orderId, String username, String stockCode, OrderType orderType,
                                        LeverageRatio leverageRatio, Integer orderPrice, Integer orderQuantity,
                                        Instant orderTime, boolean success, OrderErrorCode errorCode) {
        return new OrderResponseEvent(orderId, username, stockCode, orderType, leverageRatio, orderPrice,
                orderQuantity, orderTime, success, errorCode);
    }
}
