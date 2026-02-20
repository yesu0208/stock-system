package arile.toy.stocksystem.stockserver.autoorder.dto;

import java.time.Instant;

public record StockServerAutoOrderResponseMessage(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime
) {
    public static StockServerAutoOrderResponseMessage of(
            Long autoOrderId,
            String username,
            String stockCode,
            AutoOrderType autoOrderType,
            Integer triggerPrice,
            Integer orderPrice,
            Integer orderQuantity,
            Instant orderTime
    ) {
        return new StockServerAutoOrderResponseMessage(
                autoOrderId,
                username,
                stockCode,
                autoOrderType,
                triggerPrice,
                orderPrice,
                orderQuantity,
                orderTime
        );
    }
}
