package arile.toy.stocksystem.bffserver.chart.dto;

import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;

public record DailyCandleTickMessage(
        TickMessageType tickMessageType,
        String stockCode,
        CandleData candle
) {
    public static DailyCandleTickMessage of(String stockCode, CandleData candle) {
        return new DailyCandleTickMessage(TickMessageType.DAILY_CANDLE, stockCode, candle);
    }
}
