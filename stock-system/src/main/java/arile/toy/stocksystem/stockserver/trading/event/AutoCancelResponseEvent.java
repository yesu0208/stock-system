package arile.toy.stocksystem.stockserver.trading.event;

import arile.toy.stocksystem.stockserver.trading.dto.auto.cancel.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderType;

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
    public static AutoCancelResponseEvent of(
            Long orderId,
            String username,
            String stockCode,
            AutoOrderType autoOrderType,
            Integer triggerPrice,
            Integer orderPrice,
            Integer orderQuantity,
            boolean success,
            AutoCancelErrorCode errorCode
    ) {
        return new AutoCancelResponseEvent(
                orderId,
                username,
                stockCode,
                autoOrderType,
                triggerPrice,
                orderPrice,
                orderQuantity,
                success,
                errorCode
        );
    }
}
