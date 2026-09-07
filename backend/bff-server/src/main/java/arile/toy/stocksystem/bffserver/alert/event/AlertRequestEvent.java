package arile.toy.stocksystem.bffserver.alert.event;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertRequest;

public record AlertRequestEvent(
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice
) {
    public static AlertRequestEvent fromRequest(String username, AlertRequest request) {
        return new AlertRequestEvent(username, request.stockCode(), request.direction(), request.triggerPrice());
    }
}
