package arile.toy.stocksystem.bffserver.autoorder.dto;

import arile.toy.stocksystem.bffserver.ResponseType;

import java.time.Instant;

public record AutoOrderResultResponse(
        ResponseType responseType,
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        String errorMessage
) {
    public static AutoOrderResultResponse of(ResponseType responseType, Long autoOrderId, String username, String stockCode,
                                             AutoOrderType autoOrderType, Integer triggerPrice, Integer orderPrice, Integer orderQuantity, Instant orderTime,
                                             String errorMessage) {
        return new AutoOrderResultResponse(responseType, autoOrderId, username, stockCode, autoOrderType, triggerPrice,
                orderPrice, orderQuantity, orderTime, errorMessage);
    }
}
