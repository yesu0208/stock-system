package arile.toy.stocksystem.bffserver.external.stock.message;

import arile.toy.stocksystem.bffserver.external.stock.event.StockSummaryTickEvent;

public record StockSummaryTickMessage(
        String stockCode,
        Integer curPrice,
        Integer prevCloseDiff
) {
    public static StockSummaryTickMessage fromEvent(StockSummaryTickEvent event) {
        return new StockSummaryTickMessage(
                event.stockCode(),
                event.curPrice(),
                event.prevCloseDiff()
        );
    }
}
