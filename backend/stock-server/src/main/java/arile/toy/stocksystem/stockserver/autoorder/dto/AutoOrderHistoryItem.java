package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;

import java.time.Instant;

public record AutoOrderHistoryItem(
        Long autoOrderId,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        AutoOrderStatus autoOrderStatus,
        Instant orderTime,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice
) {
    private static final double MAINTENANCE_RATIO = 1.4;

    public static AutoOrderHistoryItem fromEntity(AutoOrderEntity entity) {
        long notionalValue = (long) entity.getOrderPrice() * entity.getOrderQuantity();

        Long initialMargin = null;
        Double maintenanceMarginRate = null;
        Long liquidationPrice = null;

        LeverageRatio leverageRatio = entity.getLeverageRatio();
        if (leverageRatio != null && !leverageRatio.isSpot()) {
            long margin = leverageRatio.calculateMarginDeposit(notionalValue);
            long loanAmount = leverageRatio.calculateLoanAmount(notionalValue);

            initialMargin = margin;
            maintenanceMarginRate = MAINTENANCE_RATIO;
            liquidationPrice = entity.getOrderQuantity() > 0
                    ? Math.round((MAINTENANCE_RATIO * loanAmount) / entity.getOrderQuantity())
                    : 0L;
        }

        return new AutoOrderHistoryItem(
                entity.getAutoOrderId(), entity.getStockCode(), entity.getAutoOrderType(), entity.getLeverageRatio(),
                entity.getTriggerPrice(), entity.getOrderPrice(), entity.getOrderQuantity(),
                entity.getAutoOrderStatus(), entity.getOrderTime(),
                notionalValue, initialMargin, maintenanceMarginRate, liquidationPrice
        );
    }
}
