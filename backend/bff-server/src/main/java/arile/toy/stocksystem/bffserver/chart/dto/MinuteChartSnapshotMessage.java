package arile.toy.stocksystem.bffserver.chart.dto;

import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;
import java.util.List;

public record MinuteChartSnapshotMessage(
        TickMessageType tickMessageType,
        String stockCode,
        List<MinuteCandle> candles
) {
    public static MinuteChartSnapshotMessage of(String stockCode, List<MinuteCandle> candles) {
        return new MinuteChartSnapshotMessage(TickMessageType.MINUTE_CHART_SNAPSHOT, stockCode, candles);
    }
}
