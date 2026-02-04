package arile.toy.stocksystem.bffserver.autoorder.event;


import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderErrorCode;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;

import java.time.Instant;

public record AutoOrderResponseEvent(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        boolean success,
        AutoOrderErrorCode errorCode
) {
}
