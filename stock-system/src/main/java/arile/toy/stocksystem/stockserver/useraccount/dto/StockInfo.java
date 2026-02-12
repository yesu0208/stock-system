package arile.toy.stocksystem.stockserver.useraccount.dto;

public record StockInfo(
        Integer quantity,
        Integer availableQuantity,
        Integer buyPrice
)
{
    public static StockInfo of(Integer quantity, Integer availableQuantity, Integer buyPrice) {
        return new StockInfo(quantity, availableQuantity, buyPrice);
    }
}
