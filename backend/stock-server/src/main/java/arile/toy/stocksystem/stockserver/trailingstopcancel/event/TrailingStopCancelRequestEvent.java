package arile.toy.stocksystem.stockserver.trailingstopcancel.event;

public record TrailingStopCancelRequestEvent(
        Long trailingStopId,
        String stockCode,
        String username
) {
    public static TrailingStopCancelRequestEvent of(Long trailingStopId, String stockCode, String username) {
        return new TrailingStopCancelRequestEvent(trailingStopId, stockCode, username);
    }
}
