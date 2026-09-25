package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.order.dto.LeverageMetrics;
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
    public static AutoOrderHistoryItem fromEntity(AutoOrderEntity entity) {
        LeverageMetrics metrics = LeverageMetrics.of(
                entity.getLeverageRatio(), entity.getOrderPrice(), entity.getOrderQuantity());

        return new AutoOrderHistoryItem(
                entity.getAutoOrderId(), entity.getStockCode(), entity.getAutoOrderType(), entity.getLeverageRatio(),
                entity.getTriggerPrice(), entity.getOrderPrice(), entity.getOrderQuantity(),
                entity.getAutoOrderStatus(), entity.getOrderTime(),
                metrics.notionalValue(), metrics.initialMargin(),
                metrics.maintenanceMarginRate(), metrics.liquidationPrice()
        );
    }
}
