package arile.toy.stocksystem.bffserver.external.stock.message;

import arile.toy.stocksystem.bffserver.external.stock.event.TradePriceTickEvent;

public record TradePriceTickMessage(
        TickMessageType tickMessageType,
        String stockCode,
        String tradeTime,
        Integer curPrice,
        Integer prevCloseDiff,
        Integer startPrice,
        Integer highPrice,
        Integer lowPrice,
        Integer tradingVolumeTick,
        Integer totalTradingVolume,
        Long totalTradingValue,
        Integer totalSellVolume,
        Integer totalBuyVolume,
        String tradingType,
        Integer prevDaySameTimeAccVolume
) {
    public static TradePriceTickMessage fromEvent(TradePriceTickEvent event) {
        return new TradePriceTickMessage(
                TickMessageType.TRADEPRICE,
                event.stockCode(),
                event.tradeTime(),
                event.curPrice(),
                event.prevCloseDiff(),
                event.startPrice(),
                event.highPrice(),
                event.lowPrice(),
                event.tradingVolumeTick(),
                event.totalTradingVolume(),
                event.totalTradingValue(),
                event.totalSellVolume(),
                event.totalBuyVolume(),
                event.tradingType(),
                event.prevDaySameTimeAccVolume()
        );
    }
}
