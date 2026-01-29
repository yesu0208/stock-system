package arile.toy.stocksystem.stockserver.external.stock.event;

import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;

public record BidAskPriceTickEvent(
        String stockCode
) {
    public static BidAskPriceTickEvent fromMessage(BidAskPriceTickMessage message) {
        return new BidAskPriceTickEvent(
                message.stockCode()
        );
    }
}
