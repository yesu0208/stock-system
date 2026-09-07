package arile.toy.stocksystem.bffserver.alert.dto;

import java.time.Instant;

public record AlertResponseMessage(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        Instant registeredTime
) {
}
