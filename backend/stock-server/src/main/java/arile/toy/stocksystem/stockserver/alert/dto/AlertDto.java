package arile.toy.stocksystem.stockserver.alert.dto;

import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;

import java.time.Instant;

public record AlertDto(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        Instant registeredTime
) {
    public static AlertDto fromEntity(AlertEntity alertEntity) {
        return new AlertDto(alertEntity.getAlertId(),
                alertEntity.getUsername(),
                alertEntity.getStockCode(),
                alertEntity.getDirection(),
                alertEntity.getTriggerPrice(),
                alertEntity.getRegisteredTime());
    }
}
