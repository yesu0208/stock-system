package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

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
        Long liquidationPrice
) {
}
