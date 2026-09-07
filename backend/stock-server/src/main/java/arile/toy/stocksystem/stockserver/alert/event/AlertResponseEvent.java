package arile.toy.stocksystem.stockserver.alert.event;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.AlertErrorCode;
import arile.toy.stocksystem.stockserver.alert.dto.StockServerAlertResponseMessage;

import java.time.Instant;

public record AlertResponseEvent(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        Instant registeredTime,
        boolean success,
        AlertErrorCode errorCode
) {
    public static AlertResponseEvent fromResponseMessage(StockServerAlertResponseMessage message) {
        return new AlertResponseEvent(message.alertId(), message.username(), message.stockCode(),
                message.direction(), message.triggerPrice(), message.registeredTime(), true, null);
    }

    public static AlertResponseEvent error(AlertRequestEvent request, AlertErrorCode errorCode) {
        return new AlertResponseEvent(null, request.username(), request.stockCode(), request.direction(),
                request.triggerPrice(), null, false, errorCode);
    }
}
