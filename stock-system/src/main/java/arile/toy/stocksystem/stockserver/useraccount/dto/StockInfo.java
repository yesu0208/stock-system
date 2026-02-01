package arile.toy.stocksystem.stockserver.useraccount.dto;

public record StockInfo(
        Integer quantity,
        Integer buyPrice
)
{
    public static StockInfo of(Integer quantity, Integer buyPrice) {
        return new  StockInfo(quantity, buyPrice);
    }
}
