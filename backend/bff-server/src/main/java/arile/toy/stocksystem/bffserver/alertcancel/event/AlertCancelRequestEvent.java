package arile.toy.stocksystem.bffserver.alertcancel.event;

import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelRequest;

public record AlertCancelRequestEvent(
        Long alertId,
        String stockCode
) {
    public static AlertCancelRequestEvent fromRequest(AlertCancelRequest alertCancelRequest) {
        return new AlertCancelRequestEvent(alertCancelRequest.alertId(), alertCancelRequest.stockCode());
    }
}
