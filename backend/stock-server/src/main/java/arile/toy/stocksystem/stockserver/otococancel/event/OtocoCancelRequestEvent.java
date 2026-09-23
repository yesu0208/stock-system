package arile.toy.stocksystem.stockserver.otococancel.event;

public record OtocoCancelRequestEvent(
        Long otocoId,
        String stockCode,
        String username
) {
    public static OtocoCancelRequestEvent of(Long otocoId, String stockCode, String username) {
        return new OtocoCancelRequestEvent(otocoId, stockCode, username);
    }
}
