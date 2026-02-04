package arile.toy.stocksystem.bffserver.autoorder.dto;

import java.time.Instant;

public record AutoOrderResponseMessage(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime
) {
}
