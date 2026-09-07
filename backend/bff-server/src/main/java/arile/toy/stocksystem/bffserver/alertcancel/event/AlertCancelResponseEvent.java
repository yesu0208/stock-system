package arile.toy.stocksystem.bffserver.alertcancel.event;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelErrorCode;

public record AlertCancelResponseEvent(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        boolean success,
        AlertCancelErrorCode errorCode
) {
}
