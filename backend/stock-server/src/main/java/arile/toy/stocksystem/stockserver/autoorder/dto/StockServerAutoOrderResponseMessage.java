package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;

import java.time.Instant;

public record StockServerAutoOrderResponseMessage(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
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
            LeverageRatio leverageRatio,
            Integer orderPrice,
            Integer orderQuantity,
            Instant orderTime
    ) {
        return new StockServerAutoOrderResponseMessage(
                autoOrderId,
                username,
                stockCode,
                autoOrderType,
                leverageRatio,
                triggerPrice,
                orderPrice,
                orderQuantity,
                orderTime
        );
    }
}
