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
        Instant orderTime,
        OrderExecutionType orderExecutionType,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice,
        OrderOrigin origin,
        Long originId
) {
    public static StockServerOrderResponseMessage of(
            Long orderId, String username, String stockCode, OrderType orderType,
            LeverageRatio leverageRatio, Integer orderPrice, Integer orderQuantity,
            Integer remainingQuantity, Instant orderTime, OrderExecutionType orderExecutionType,
            OrderOrigin origin, Long originId
    ) {
        LeverageMetrics metrics = LeverageMetrics.of(leverageRatio, orderPrice, orderQuantity);

        return new StockServerOrderResponseMessage(
                orderId, username, stockCode, orderType, leverageRatio,
                orderPrice, orderQuantity, remainingQuantity, orderTime, orderExecutionType,
                metrics.notionalValue(), metrics.initialMargin(),
                metrics.maintenanceMarginRate(), metrics.liquidationPrice(),
                origin, originId
        );
    }
}
