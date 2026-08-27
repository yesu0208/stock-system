package arile.toy.stocksystem.bffserver.chart.event;

import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandle;

public record MinuteCandleUpdateEvent(
        String stockCode,
        MinuteCandle candle
) {
}
