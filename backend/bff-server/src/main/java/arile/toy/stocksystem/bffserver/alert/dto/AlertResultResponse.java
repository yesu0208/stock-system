package arile.toy.stocksystem.bffserver.alert.dto;

import arile.toy.stocksystem.bffserver.ResponseType;

import java.time.Instant;

public record AlertResultResponse(
        ResponseType responseType,
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        Instant registeredTime,
        String errorMessage
) {
    public static AlertResultResponse of(ResponseType responseType, Long alertId, String username, String stockCode,
                                         AlertDirection direction, Integer triggerPrice, Instant registeredTime,
                                         String errorMessage) {
        return new AlertResultResponse(responseType, alertId, username, stockCode, direction, triggerPrice,
                registeredTime, errorMessage);
    }
}
