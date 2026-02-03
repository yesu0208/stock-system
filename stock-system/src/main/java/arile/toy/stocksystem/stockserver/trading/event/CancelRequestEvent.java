package arile.toy.stocksystem.stockserver.trading.event;

public record CancelRequestEvent(
        Long orderId,
        String stockCode
) {
    public static CancelRequestEvent of(Long orderId, String stockCode) {
        return new CancelRequestEvent(orderId, stockCode);
    }
}
