package arile.toy.stocksystem.bffserver.chart.event;

import arile.toy.stocksystem.bffserver.chart.dto.CandleData;

public record DailyCandleUpdateEvent(
        String stockCode,
        CandleData candle
) {
}
