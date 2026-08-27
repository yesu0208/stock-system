package arile.toy.stocksystem.stockserver.chart.event;

import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;

public record MinuteCandleUpdateEvent(
        String stockCode,
        MinuteCandle candle
) {
    public static MinuteCandleUpdateEvent of(String stockCode, MinuteCandle candle) {
        return new MinuteCandleUpdateEvent(stockCode, candle);
    }
}
