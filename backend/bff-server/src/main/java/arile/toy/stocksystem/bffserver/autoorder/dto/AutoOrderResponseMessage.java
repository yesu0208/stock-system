package arile.toy.stocksystem.bffserver.autoorder.dto;

import arile.toy.stocksystem.bffserver.order.dto.LeverageRatio;

import java.time.Instant;

public record AutoOrderResponseMessage(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime
) {
}
