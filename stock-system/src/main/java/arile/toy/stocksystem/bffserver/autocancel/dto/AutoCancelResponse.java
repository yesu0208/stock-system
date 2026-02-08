package arile.toy.stocksystem.bffserver.autocancel.dto;

public record AutoCancelResponse(
        Long autoOrderId,
        String stockCode
) {
}
