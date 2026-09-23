package arile.toy.stocksystem.stockserver.alertcancel.event;

public record AlertCancelRequestEvent(
        Long alertId,
        String stockCode,
        String username
) {
    public static AlertCancelRequestEvent of(Long alertId, String stockCode, String username) {
        return new AlertCancelRequestEvent(alertId, stockCode, username);
    }
}
