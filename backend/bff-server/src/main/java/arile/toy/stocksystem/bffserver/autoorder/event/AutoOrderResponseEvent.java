package arile.toy.stocksystem.bffserver.autoorder.event;


import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record AutoOrderResponseEvent(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        boolean success,
        AutoOrderResultCode resultCode
) {
}
