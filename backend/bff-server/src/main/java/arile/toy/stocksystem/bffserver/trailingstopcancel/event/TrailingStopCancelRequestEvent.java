package arile.toy.stocksystem.bffserver.trailingstopcancel.event;

import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelRequest;

public record TrailingStopCancelRequestEvent(
        Long trailingStopId,
        String stockCode
) {
    public static TrailingStopCancelRequestEvent fromRequest(TrailingStopCancelRequest request) {
        return new TrailingStopCancelRequestEvent(request.trailingStopId(), request.stockCode());
    }
}
