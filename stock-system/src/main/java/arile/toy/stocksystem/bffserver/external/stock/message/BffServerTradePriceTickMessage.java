package arile.toy.stocksystem.bffserver.external.stock.message;

public record BffServerTradePriceTickMessage(
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
