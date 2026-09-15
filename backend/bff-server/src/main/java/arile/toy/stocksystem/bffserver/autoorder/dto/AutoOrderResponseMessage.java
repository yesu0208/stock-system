package arile.toy.stocksystem.bffserver.autoorder.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record AutoOrderResponseMessage(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice
) {
    private static final double MAINTENANCE_RATIO = 1.4;

    private static double marginRateOf(LeverageRatio ratio) {
        return switch (ratio) {
            case SPOT -> 1.0;
            case X1_5 -> 0.667;
            case X2 -> 0.5;
            case X2_5 -> 0.4;
        };
    }

    public static AutoOrderResponseMessage of(
            Long autoOrderId, String username, String stockCode, AutoOrderType autoOrderType,
            LeverageRatio leverageRatio, Integer triggerPrice, Integer orderPrice,
            Integer orderQuantity, Instant orderTime
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

        return new AutoOrderResponseMessage(
                autoOrderId, username, stockCode, autoOrderType, leverageRatio,
                triggerPrice, orderPrice, orderQuantity, orderTime,
                notionalValue, initialMargin, maintenanceMarginRate, liquidationPrice
        );
    }
}
