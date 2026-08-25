package arile.toy.stocksystem.bffserver.autocancel.event;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelRequest;

public record AutoCancelRequestEvent(
        Long autoOrderId,
        String stockCode
) {
    public static AutoCancelRequestEvent fromRequest(AutoCancelRequest autoCancelRequest) {
        return new AutoCancelRequestEvent(autoCancelRequest.autoOrderId(), autoCancelRequest.stockCode());
    }
}
