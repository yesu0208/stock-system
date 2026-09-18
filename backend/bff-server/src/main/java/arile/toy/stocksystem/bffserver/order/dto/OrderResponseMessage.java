package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

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
        Instant orderTime,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice,
        OrderExecutionType orderExecutionType
) {
    private static final double MAINTENANCE_RATIO = 1.4;

    private static double marginRateOf(LeverageRatio ratio) {
        return switch (ratio) {
            case SPOT -> 1.0;
            case X1_5 -> 1.0 / 1.5;
            case X2 -> 0.5;
            case X2_5 -> 0.4;
        };
    }

    public static OrderResponseMessage of(
            Long orderId, String username, String stockCode, OrderType orderType,
            LeverageRatio leverageRatio, Integer orderPrice, Integer orderQuantity,
            Integer remainingQuantity, Instant orderTime, OrderExecutionType orderExecutionType
    ) {
        long notionalValue = (long) orderPrice * orderQuantity;

        Long initialMargin = null;
        Double maintenanceMarginRate = null;
        Long liquidationPrice = null;

        if (leverageRatio != null && !leverageRatio.isSpot()) {
            long margin = Math.round(notionalValue * marginRateOf(leverageRatio));
            long loanAmount = notionalValue - margin;

            initialMargin = margin;
            maintenanceMarginRate = MAINTENANCE_RATIO;
            liquidationPrice = orderQuantity > 0
                    ? Math.round((MAINTENANCE_RATIO * loanAmount) / orderQuantity)
                    : 0L;
        }

        return new OrderResponseMessage(
                orderId, username, stockCode, orderType, leverageRatio,
                orderPrice, orderQuantity, remainingQuantity, orderTime,
                notionalValue, initialMargin, maintenanceMarginRate, liquidationPrice,
                orderExecutionType
        );
    }
}
