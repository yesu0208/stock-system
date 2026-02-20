package arile.toy.stocksystem.bffserver.cancel.event;

import arile.toy.stocksystem.bffserver.cancel.dto.CancelRequest;

public record CancelRequestEvent(
        Long orderId,
        String stockCode
) {
    public static CancelRequestEvent fromRequest(CancelRequest cancelRequest) {
        return new CancelRequestEvent(cancelRequest.orderId(), cancelRequest.stockCode());
    }
}
