package arile.toy.stocksystem.stockserver.external.stock.message;

public record StockSummaryTickMessage(
        String stockCode,
        Integer curPrice,
        Integer prevCloseDiff
) {
}
