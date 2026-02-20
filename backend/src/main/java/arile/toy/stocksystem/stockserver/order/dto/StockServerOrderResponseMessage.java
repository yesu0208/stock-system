package arile.toy.stocksystem.stockserver.order.dto;

import java.time.Instant;

public record StockServerOrderResponseMessage(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
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
                orderPrice,
                orderQuantity,
                remainingQuantity,
                orderTime
        );
    }
}
