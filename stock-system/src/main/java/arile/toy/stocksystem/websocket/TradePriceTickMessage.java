package arile.toy.stocksystem.websocket;

public record TradePriceTickMessage(
        String type,
        String code,
        String time,
        String curPrice,
        String prevCloseDiff,
        String startPrice,
        String highPrice,
        String lowPrice,
        String tradingVolumeTick,
        String totalTradingVolume,
        String totalTradingValue,
        String totalSellVolume,
        String totalBuyVolume,
        String tradingType,
        String prevDaySameTimeAccVolume
) {}
