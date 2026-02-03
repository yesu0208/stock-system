package arile.toy.stocksystem.bffserver.cancel.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;

public record CancelResultResponse(
        ResponseType responseType,
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        String errorMessage
) {
    public static CancelResultResponse of(ResponseType responseType, Long orderId, String username, String stockCode,
                                                                                   OrderType orderType, Integer orderPrice, Integer orderQuantity,
                                                                                   String errorMessage) {
        return new CancelResultResponse(responseType, orderId, username, stockCode, orderType,
                orderPrice, orderQuantity, errorMessage);
    }
}
