package arile.toy.stocksystem.bffserver.order.event;

import arile.toy.stocksystem.bffserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.order.dto.OrderErrorCode;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;

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
}
