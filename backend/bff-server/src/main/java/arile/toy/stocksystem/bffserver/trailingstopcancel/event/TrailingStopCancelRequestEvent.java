package arile.toy.stocksystem.bffserver.trailingstopcancel.event;

import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelRequest;

public record TrailingStopCancelRequestEvent(
        Long trailingStopId,
        String stockCode,
        String username
) {
    public static TrailingStopCancelRequestEvent fromRequest(String username, TrailingStopCancelRequest request) {
        return new TrailingStopCancelRequestEvent(request.trailingStopId(), request.stockCode(), username);
    }
}
