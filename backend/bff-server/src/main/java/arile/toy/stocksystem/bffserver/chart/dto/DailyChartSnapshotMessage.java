package arile.toy.stocksystem.bffserver.chart.dto;

import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;

import java.util.List;

public record DailyChartSnapshotMessage(
        TickMessageType tickMessageType,
        String stockCode,
        List<CandleData> candles
) {
    public static DailyChartSnapshotMessage of(String stockCode, List<CandleData> candles) {
        return new DailyChartSnapshotMessage(TickMessageType.DAILY_CHART_SNAPSHOT, stockCode, candles);
    }
}
