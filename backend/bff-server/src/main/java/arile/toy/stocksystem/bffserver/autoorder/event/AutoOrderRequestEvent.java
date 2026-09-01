package arile.toy.stocksystem.bffserver.autoorder.event;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.order.dto.LeverageRatio;

public record AutoOrderRequestEvent(
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio
) {
}
