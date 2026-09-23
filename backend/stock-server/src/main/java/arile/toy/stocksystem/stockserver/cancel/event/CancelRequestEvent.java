package arile.toy.stocksystem.stockserver.cancel.event;

public record CancelRequestEvent(
        Long orderId,
        String stockCode,
        String username
) {
    public static CancelRequestEvent of(Long orderId, String stockCode, String username) {
        return new CancelRequestEvent(orderId, stockCode, username);
    }
}
