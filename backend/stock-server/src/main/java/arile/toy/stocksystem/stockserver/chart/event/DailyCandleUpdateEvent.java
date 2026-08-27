package arile.toy.stocksystem.stockserver.chart.event;

import arile.toy.stocksystem.stockserver.chart.dto.CandleData;

public record DailyCandleUpdateEvent(
        String stockCode,
        CandleData candle
) {
    public static DailyCandleUpdateEvent of(String stockCode, CandleData candle) {
        return new DailyCandleUpdateEvent(stockCode, candle);
    }
}
