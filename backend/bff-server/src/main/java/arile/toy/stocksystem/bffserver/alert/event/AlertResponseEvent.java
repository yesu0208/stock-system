package arile.toy.stocksystem.bffserver.alert.event;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertErrorCode;

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
}
