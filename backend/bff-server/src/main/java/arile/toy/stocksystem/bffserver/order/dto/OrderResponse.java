package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

public record OrderResponse(
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio
) {
}
