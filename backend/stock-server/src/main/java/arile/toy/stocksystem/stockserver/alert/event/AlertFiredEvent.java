package arile.toy.stocksystem.stockserver.alert.event;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.AlertDto;

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
    public static AlertFiredEvent of(AlertDto alertDto, Integer currentPrice) {
        return new AlertFiredEvent(alertDto.alertId(), alertDto.username(), alertDto.stockCode(),
                alertDto.direction(), alertDto.triggerPrice(), currentPrice, Instant.now());
    }
}
