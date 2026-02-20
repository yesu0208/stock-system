package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.stockserver.order.dto.OrderType;

import java.time.Instant;

public record OrderResponseMessage(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        Integer remainingQuantity,
        Instant orderTime
) {
}