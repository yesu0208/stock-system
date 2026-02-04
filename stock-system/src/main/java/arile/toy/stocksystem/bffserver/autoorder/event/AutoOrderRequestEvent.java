package arile.toy.stocksystem.bffserver.autoorder.event;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;

public record AutoOrderRequestEvent(
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity
) {
}
