package arile.toy.stocksystem.stockserver.external.stock.message;

import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;

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
}
