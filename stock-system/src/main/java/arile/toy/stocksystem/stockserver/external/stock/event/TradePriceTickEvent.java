package arile.toy.stocksystem.stockserver.external.stock.event;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;

public record TradePriceTickEvent(
        String stockCode
) {
    public static TradePriceTickEvent fromMessage(TradePriceTickMessage message) {
        return new TradePriceTickEvent(
                message.stockCode()
        );
    }
}
