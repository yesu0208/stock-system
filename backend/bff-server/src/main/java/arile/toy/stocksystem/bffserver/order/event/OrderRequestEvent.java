package arile.toy.stocksystem.bffserver.order.event;

import arile.toy.stocksystem.bffserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;

public record OrderRequestEvent(
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio
) {
}
