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
    private static final double MAINTENANCE_RATIO = 1.4;

    public static StockServerOrderResponseMessage of(
            Long orderId, String username, String stockCode, OrderType orderType,
            LeverageRatio leverageRatio, Integer orderPrice, Integer orderQuantity,
            Integer remainingQuantity, Instant orderTime, OrderExecutionType orderExecutionType,
            OrderOrigin origin, Long originId
    ) {
        long notionalValue = (long) orderPrice * orderQuantity;

        Long initialMargin = null;
        Double maintenanceMarginRate = null;
        Long liquidationPrice = null;

        if (leverageRatio != null && !leverageRatio.isSpot()) {
            long margin = leverageRatio.calculateMarginDeposit(notionalValue);
            long loanAmount = leverageRatio.calculateLoanAmount(notionalValue);

            initialMargin = margin;
            maintenanceMarginRate = MAINTENANCE_RATIO;
            liquidationPrice = orderQuantity > 0
                    ? Math.round((MAINTENANCE_RATIO * loanAmount) / orderQuantity)
                    : 0L;
        }

        return new StockServerOrderResponseMessage(
                orderId, username, stockCode, orderType, leverageRatio,
                orderPrice, orderQuantity, remainingQuantity, orderTime, orderExecutionType,
                notionalValue, initialMargin, maintenanceMarginRate, liquidationPrice,
                origin, originId
        );
    }
}
