package arile.toy.stocksystem.bffserver.chart.dto;

import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;

public record MinuteCandleTickMessage(
        TickMessageType tickMessageType,
        String stockCode,
        MinuteCandle candle
) {
    public static MinuteCandleTickMessage of(String stockCode, MinuteCandle candle) {
        return new MinuteCandleTickMessage(TickMessageType.MINUTE_CANDLE, stockCode, candle);
    }
}
