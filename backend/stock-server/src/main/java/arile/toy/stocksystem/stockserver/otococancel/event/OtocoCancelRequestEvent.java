package arile.toy.stocksystem.stockserver.otococancel.event;

public record OtocoCancelRequestEvent(
        Long otocoId,
        String stockCode
) {
    public static OtocoCancelRequestEvent of(Long otocoId, String stockCode) {
        return new OtocoCancelRequestEvent(otocoId, stockCode);
    }
}
