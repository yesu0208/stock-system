package arile.toy.stocksystem.bffserver.order.dto;

import java.time.Instant;

public record OrderResponseMessage(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer orderQuantity,
        Integer remainingQuantity,
        Instant orderTime
) {
}
