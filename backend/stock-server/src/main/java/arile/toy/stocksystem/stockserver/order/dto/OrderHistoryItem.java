package arile.toy.stocksystem.stockserver.order.dto;

import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;

import java.time.Instant;

public record OrderHistoryItem(
        Long orderId,
        String stockCode,
        OrderType orderType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer orderQuantity,
        Integer remainingQuantity,
        OrderStatus orderStatus,
        Instant orderTime,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice,
        OrderExecutionType orderExecutionType,
        OrderOrigin origin,
        Long originId
) {
    private static final double MAINTENANCE_RATIO = 1.4;

    public static OrderHistoryItem fromEntity(OrderEntity entity) {
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

        return new OrderHistoryItem(
                entity.getOrderId(), entity.getStockCode(), entity.getOrderType(), entity.getLeverageRatio(),
                entity.getOrderPrice(), entity.getOrderQuantity(), entity.getRemainingQuantity(),
                entity.getOrderStatus(), entity.getOrderTime(),
                notionalValue, initialMargin, maintenanceMarginRate, liquidationPrice,
                entity.getOrderExecutionType(),
                entity.getOrigin(),
                entity.getOriginId()
        );
    }
}
