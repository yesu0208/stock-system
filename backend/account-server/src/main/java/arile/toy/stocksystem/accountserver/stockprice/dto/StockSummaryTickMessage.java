package arile.toy.stocksystem.accountserver.stockprice.dto;

public record StockSummaryTickMessage(
        String stockCode,
        Integer curPrice,
        Integer prevCloseDiff
) {
}
