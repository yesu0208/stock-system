package arile.toy.stocksystem.bffserver.autocancel.event;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;

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
