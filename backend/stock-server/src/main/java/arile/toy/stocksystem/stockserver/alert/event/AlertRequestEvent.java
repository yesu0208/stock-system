package arile.toy.stocksystem.stockserver.alert.event;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;

public record AlertRequestEvent(
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice
) {
    public static AlertRequestEvent of(String username, String stockCode, AlertDirection direction, Integer triggerPrice) {
        return new AlertRequestEvent(username, stockCode, direction, triggerPrice);
    }
}
