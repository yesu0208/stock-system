package arile.toy.stocksystem.stockserver.external.stock.event;

import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;

public record TradePriceTickEvent(
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
    public static TradePriceTickEvent fromMessage(TradePriceTickMessage message) {
        return new TradePriceTickEvent(
                message.tickMessageType(),
                message.stockCode(),
                message.tradeTime(),
                message.curPrice(),
                message.prevCloseDiff(),
                message.startPrice(),
                message.highPrice(),
                message.lowPrice(),
                message.tradingVolumeTick(),
                message.totalTradingVolume(),
                message.totalTradingValue(),
                message.totalSellVolume(),
                message.totalBuyVolume(),
                message.tradingType(),
                message.prevDaySameTimeAccVolume()
        );
    }
}
