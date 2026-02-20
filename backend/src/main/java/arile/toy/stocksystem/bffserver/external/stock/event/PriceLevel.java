package arile.toy.stocksystem.bffserver.external.stock.event;

public record PriceLevel(
        Integer price,
        Integer quantity
) {
}