package arile.toy.stocksystem.stockserver.alert.dto;

import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;

public record UpdateAlertStatusResult(
        AlertEntity alertEntity,
        AlertStatus previousStatus
) {
    public static UpdateAlertStatusResult of(AlertEntity alertEntity, AlertStatus previousStatus) {
        return new UpdateAlertStatusResult(alertEntity, previousStatus);
    }
}
