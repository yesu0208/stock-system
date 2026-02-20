package arile.toy.stocksystem.bffserver.external.stock.message;

public record BffServerTradePriceClientTickMessage(
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
        Integer prevDaySameTimeAccVolume,
        Integer prevClosePrice
) {
    public static BffServerTradePriceClientTickMessage fromTickMessage(BffServerTradePriceTickMessage bffServerTradePriceClientMessage) {
        return new BffServerTradePriceClientTickMessage(
                bffServerTradePriceClientMessage.tickMessageType(),
                bffServerTradePriceClientMessage.stockCode(),
                bffServerTradePriceClientMessage.tradeTime(),
                bffServerTradePriceClientMessage.curPrice(),
                bffServerTradePriceClientMessage.prevCloseDiff(),
                bffServerTradePriceClientMessage.startPrice(),
                bffServerTradePriceClientMessage.highPrice(),
                bffServerTradePriceClientMessage.lowPrice(),
                bffServerTradePriceClientMessage.tradingVolumeTick(),
                bffServerTradePriceClientMessage.totalTradingVolume(),
                bffServerTradePriceClientMessage.totalTradingValue(),
                bffServerTradePriceClientMessage.totalSellVolume(),
                bffServerTradePriceClientMessage.totalBuyVolume(),
                bffServerTradePriceClientMessage.tradingType(),
                bffServerTradePriceClientMessage.prevDaySameTimeAccVolume(),
                bffServerTradePriceClientMessage.curPrice() - bffServerTradePriceClientMessage.prevCloseDiff()
        );
    }
}
