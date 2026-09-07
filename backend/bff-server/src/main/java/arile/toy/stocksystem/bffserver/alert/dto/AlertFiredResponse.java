package arile.toy.stocksystem.bffserver.alert.dto;

import java.time.Instant;

public record AlertFiredResponse(
        Long alertId,
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice,
        Integer currentPrice,
        Instant firedTime
) {
}
