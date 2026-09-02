package arile.toy.stocksystem.stockserver.order.dto;

import java.time.Instant;

public record StockServerOrderResponseMessage(
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
    public static StockServerOrderResponseMessage of(
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
        return new StockServerOrderResponseMessage(
                orderId,
                username,
                stockCode,
                orderType,
                leverageRatio,
                orderPrice,
                orderQuantity,
                remainingQuantity,
                orderTime
        );
    }
}
