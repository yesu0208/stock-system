package arile.toy.stocksystem.bffserver.external.stock.message;

public record BffServerStockSummaryClientTickMessage(
        String stockCode,
        Integer curPrice,
        Integer prevClose
) {
    public static BffServerStockSummaryClientTickMessage fromBiffServerStockSummaryTickMessage
            (BffServerStockSummaryTickMessage bffServerStockSummaryTickMessage){
        return new BffServerStockSummaryClientTickMessage(
                bffServerStockSummaryTickMessage.stockCode(),
                bffServerStockSummaryTickMessage.curPrice(),
                bffServerStockSummaryTickMessage.curPrice() - bffServerStockSummaryTickMessage.prevCloseDiff()
        );
    }
}
