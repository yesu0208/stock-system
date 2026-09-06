package arile.toy.stocksystem.stockserver.trailingstopcancel.event;

public record TrailingStopCancelRequestEvent(
        Long trailingStopId,
        String stockCode
) {
    public static TrailingStopCancelRequestEvent of(Long trailingStopId, String stockCode) {
        return new TrailingStopCancelRequestEvent(trailingStopId, stockCode);
    }
}
