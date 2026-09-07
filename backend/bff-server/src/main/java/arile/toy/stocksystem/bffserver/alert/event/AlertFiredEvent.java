package arile.toy.stocksystem.bffserver.alert.event;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;

import java.time.Instant;

public record AlertFiredEvent(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        Integer currentPrice,
        Instant firedTime
) {
}
