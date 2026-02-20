package arile.toy.stocksystem.stockserver.autocancel.event;

import arile.toy.stocksystem.stockserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;

public record AutoCancelResponseEvent(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        boolean success,
        AutoCancelErrorCode errorCode
) {
    public static AutoCancelResponseEvent of(AutoOrderEntity autoOrder, boolean success, AutoCancelErrorCode errorCode) {
        return new AutoCancelResponseEvent(
                autoOrder.getAutoOrderId(),
                autoOrder.getUsername(),
                autoOrder.getStockCode(),
                autoOrder.getAutoOrderType(),
                autoOrder.getTriggerPrice(),
                autoOrder.getOrderPrice(),
                autoOrder.getOrderQuantity(),
                success,
                errorCode
        );
    }
}
