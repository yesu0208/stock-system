package arile.toy.stocksystem.bffserver.cancel.event;


import arile.toy.stocksystem.bffserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;

public record CancelResponseEvent(
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        boolean success,
        CancelErrorCode errorCode
) {
    public static CancelResponseEvent of(
            Long orderId,
            String username,
            String stockCode,
            OrderType orderType,
            Integer orderPrice,
            Integer orderQuantity,
            boolean success,
            CancelErrorCode errorCode
    ) {
        return new CancelResponseEvent(
                orderId,
                username,
                stockCode,
                orderType,
                orderPrice,
                orderQuantity,
                success,
                errorCode
        );
    }
}
