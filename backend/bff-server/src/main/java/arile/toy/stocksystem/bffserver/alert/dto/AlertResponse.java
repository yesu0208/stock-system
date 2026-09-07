package arile.toy.stocksystem.bffserver.alert.dto;

public record AlertResponse(
        String username,
        String stockCode,
        AlertDirection direction,
        Integer triggerPrice
) {
}
