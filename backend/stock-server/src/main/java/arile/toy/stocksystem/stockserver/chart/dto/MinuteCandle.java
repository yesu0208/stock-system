package arile.toy.stocksystem.stockserver.chart.dto;

public record MinuteCandle(
        String date,
        String time,
        long open,
        long high,
        long low,
        long close,
        long volume
) {
}
