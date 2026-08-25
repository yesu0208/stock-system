package arile.toy.stocksystem.bffserver.external.stock.message;

public record BffServerStockSummaryTickMessage(
        String stockCode,
        Integer curPrice,
        Integer prevCloseDiff
) {
}
