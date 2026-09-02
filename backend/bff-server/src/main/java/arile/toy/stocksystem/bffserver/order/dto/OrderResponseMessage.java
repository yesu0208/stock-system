package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record OrderResponseMessage(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer orderQuantity,
        Integer remainingQuantity,
        Instant orderTime
) {
}
