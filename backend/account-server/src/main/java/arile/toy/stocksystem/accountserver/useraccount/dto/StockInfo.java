package arile.toy.stocksystem.accountserver.useraccount.dto;

public record StockInfo(
        Integer quantity,
        Integer availableQuantity,
        Long totalAmount
) {
    public static StockInfo of(Integer quantity, Integer availableQuantity, Long totalAmount) {
        return new StockInfo(quantity, availableQuantity, totalAmount);
    }
}
