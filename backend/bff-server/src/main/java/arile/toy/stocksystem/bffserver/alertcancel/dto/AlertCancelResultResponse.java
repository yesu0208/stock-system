package arile.toy.stocksystem.bffserver.alertcancel.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;

public record AlertCancelResultResponse(
        ResponseType responseType,
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        String errorMessage
) {
    public static AlertCancelResultResponse of(ResponseType responseType, Long alertId, String username, String stockCode,
                                               AlertDirection direction, Integer triggerPrice, String errorMessage) {
        return new AlertCancelResultResponse(responseType, alertId, username, stockCode, direction, triggerPrice, errorMessage);
    }
}
