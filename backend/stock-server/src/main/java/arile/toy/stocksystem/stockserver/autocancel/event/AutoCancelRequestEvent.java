package arile.toy.stocksystem.stockserver.autocancel.event;

public record AutoCancelRequestEvent(
        Long autoOrderId,
        String stockCode,
        String username
) {
    public static AutoCancelRequestEvent of(Long autoOrderId, String stockCode, String username) {
        return new AutoCancelRequestEvent(autoOrderId, stockCode, username);
    }
}
