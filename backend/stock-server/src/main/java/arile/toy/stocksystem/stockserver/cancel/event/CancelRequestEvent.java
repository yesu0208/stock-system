package arile.toy.stocksystem.stockserver.cancel.event;

public record CancelRequestEvent(
        Long orderId,
        String stockCode
) {
    public static CancelRequestEvent of(Long orderId, String stockCode) {
        return new CancelRequestEvent(orderId, stockCode);
    }
}
