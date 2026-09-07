package arile.toy.stocksystem.stockserver.alert.dto;

import java.time.Instant;

public record StockServerAlertResponseMessage(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        Instant registeredTime
) {
    public static StockServerAlertResponseMessage of(
            Long alertId,
            String username,
            String stockCode,
            AlertDirection direction,
            Integer triggerPrice,
            Instant registeredTime
    ) {
        return new StockServerAlertResponseMessage(alertId, username, stockCode, direction, triggerPrice, registeredTime);
    }
}
