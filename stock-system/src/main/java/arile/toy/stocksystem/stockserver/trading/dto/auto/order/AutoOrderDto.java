package arile.toy.stocksystem.stockserver.trading.dto.auto.order;

import java.time.Instant;

public record AutoOrderDto(
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
