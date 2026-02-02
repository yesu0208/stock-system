package arile.toy.stocksystem.bffserver.cancel.dto;

public record CancelResponse(
        Long orderId,
        String stockCode
) {
}
