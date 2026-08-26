package arile.toy.stocksystem.bffserver.stockinfo.dto;

import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;

public record StockDetailTickMessage(
        TickMessageType tickMessageType,
        String stockCode,
        String stockName,
        String market,
        String baseTime,
        String currentPrice,
        String diffPrice,
        String diffRate,
        String direction,
        String prevPrice,
        String openPrice,
        String highPrice,
        String upperLimit,
        String lowPrice,
        String lowerLimit,
        String volume,
        String tradingValue
) {
    public static StockDetailTickMessage of(
            String stockCode, String stockName, String market, String baseTime,
            String currentPrice, String diffPrice, String diffRate, String direction,
            String prevPrice, String openPrice, String highPrice, String upperLimit,
            String lowPrice, String lowerLimit, String volume, String tradingValue
    ) {
        return new StockDetailTickMessage(TickMessageType.DETAIL, stockCode, stockName, market, baseTime,
                currentPrice, diffPrice, diffRate, direction, prevPrice, openPrice, highPrice, upperLimit,
                lowPrice, lowerLimit, volume, tradingValue);
    }
}
