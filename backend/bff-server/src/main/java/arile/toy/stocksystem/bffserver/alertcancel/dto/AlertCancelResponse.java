package arile.toy.stocksystem.bffserver.alertcancel.dto;

public record AlertCancelResponse(
        Long alertId,
        String stockCode
) {
}
