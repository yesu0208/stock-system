package arile.toy.stocksystem.accountserver.useraccount.dto;

public record StockInfo(
        Integer quantity,
        Integer availableQuantity,
        Long totalAmount,
        Long totalCostAmount
) {
    public static StockInfo of(Integer quantity, Integer availableQuantity, Long totalAmount, Long totalCostAmount) { // [수정]
        return new StockInfo(quantity, availableQuantity, totalAmount, totalCostAmount);
    }
}
