package arile.toy.stocksystem.stockserver.external.stock.event;

import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;

public record StockSummaryTickEvent(
        String stockCode,
        Integer curPrice,
        Integer prevCloseDiff
) {
    public static StockSummaryTickEvent fromMessage(StockSummaryTickMessage message) {
        return new StockSummaryTickEvent(
                message.stockCode(),
                message.curPrice(),
                message.prevCloseDiff()
        );
    }
}
