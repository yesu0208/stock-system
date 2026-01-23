package arile.toy.stocksystem.stockserver.external.stock.event;

public record PriceLevel(
        Integer price,
        Integer quantity
) {
}