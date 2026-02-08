package arile.toy.stocksystem.stockserver.trading.event;

public record AutoCancelRequestEvent(
        Long autoOrderId,
        String stockCode
) {
    public static AutoCancelRequestEvent of(Long autoOrderId, String stockCode) {
        return new AutoCancelRequestEvent(autoOrderId, stockCode);
    }
}
