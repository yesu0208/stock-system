package arile.toy.stocksystem.stockserver.alertcancel.event;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alertcancel.dto.AlertCancelErrorCode;

public record AlertCancelResponseEvent(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        boolean success,
        AlertCancelErrorCode errorCode
) {
    public static AlertCancelResponseEvent of(AlertEntity alertEntity, boolean success, AlertCancelErrorCode errorCode) {
        return new AlertCancelResponseEvent(
                alertEntity.getAlertId(),
                alertEntity.getUsername(),
                alertEntity.getStockCode(),
                alertEntity.getDirection(),
                alertEntity.getTriggerPrice(),
                success,
                errorCode
        );
    }
}
