package arile.toy.stocksystem.bffserver.account.dto;

public record StockInfo(
        Integer quantity,
        Integer availableQuantity,
        Integer buyPrice

) {
}
