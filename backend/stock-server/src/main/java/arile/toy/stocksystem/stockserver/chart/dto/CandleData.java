package arile.toy.stocksystem.stockserver.chart.dto;

public record CandleData(
        String date,
        long open,
        long high,
        long low,
        long close,
        long volume
) {
}
