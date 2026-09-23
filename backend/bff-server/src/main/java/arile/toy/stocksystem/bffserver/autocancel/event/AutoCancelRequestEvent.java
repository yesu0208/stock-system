package arile.toy.stocksystem.bffserver.autocancel.event;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelRequest;

public record AutoCancelRequestEvent(
        Long autoOrderId,
        String stockCode,
        String username
) {
    public static AutoCancelRequestEvent fromRequest(String username, AutoCancelRequest autoCancelRequest) {
        return new AutoCancelRequestEvent(autoCancelRequest.autoOrderId(), autoCancelRequest.stockCode(), username);
    }
}
