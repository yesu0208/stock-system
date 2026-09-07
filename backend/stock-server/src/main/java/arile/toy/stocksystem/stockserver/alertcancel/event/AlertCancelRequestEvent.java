package arile.toy.stocksystem.stockserver.alertcancel.event;

public record AlertCancelRequestEvent(
        Long alertId,
        String stockCode
) {
    public static AlertCancelRequestEvent of(Long alertId, String stockCode) {
        return new AlertCancelRequestEvent(alertId, stockCode);
    }
}
