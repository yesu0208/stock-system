package arile.toy.stocksystem.bffserver.autocancel.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;

public record AutoCancelResultResponse(
        ResponseType responseType,
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        String errorMessage
) {
    public static AutoCancelResultResponse of(ResponseType responseType, Long autoOrderId, String username, String stockCode,
                                              AutoOrderType autoOrderType, Integer triggerPrice, Integer orderPrice, Integer orderQuantity,
                                              String errorMessage) {
        return new AutoCancelResultResponse(responseType, autoOrderId, username, stockCode, autoOrderType, triggerPrice,
                orderPrice, orderQuantity, errorMessage);
    }
}
