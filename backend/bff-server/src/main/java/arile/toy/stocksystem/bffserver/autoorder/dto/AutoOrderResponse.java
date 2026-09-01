package arile.toy.stocksystem.bffserver.autoorder.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

public record AutoOrderResponse(
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio
) {
}
