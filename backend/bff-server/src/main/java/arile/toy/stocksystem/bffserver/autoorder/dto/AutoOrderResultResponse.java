package arile.toy.stocksystem.bffserver.autoorder.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record AutoOrderResultResponse(
        ResponseType responseType,
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        String errorMessage,
        AutoOrderResultCode resultCode
) {
    public static AutoOrderResultResponse of(ResponseType responseType, Long autoOrderId, String username, String stockCode,
                                             AutoOrderType autoOrderType, LeverageRatio leverageRatio,
                                             Integer triggerPrice, Integer orderPrice, Integer orderQuantity, Instant orderTime,
                                             String errorMessage) {
        return of(responseType, autoOrderId, username, stockCode, autoOrderType, leverageRatio, triggerPrice,
                orderPrice, orderQuantity, orderTime, errorMessage, null);
    }

    public static AutoOrderResultResponse of(ResponseType responseType, Long autoOrderId, String username, String stockCode,
                                             AutoOrderType autoOrderType, LeverageRatio leverageRatio,
                                             Integer triggerPrice, Integer orderPrice, Integer orderQuantity, Instant orderTime,
                                             String errorMessage, AutoOrderResultCode resultCode) {
        return new AutoOrderResultResponse(responseType, autoOrderId, username, stockCode, autoOrderType, leverageRatio, triggerPrice,
                orderPrice, orderQuantity, orderTime, errorMessage, resultCode);
    }
}
