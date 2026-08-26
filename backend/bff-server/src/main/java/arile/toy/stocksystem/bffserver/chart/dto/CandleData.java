package arile.toy.stocksystem.bffserver.chart.dto;

public record CandleData(

        String date,

        long open,
        long high,
        long low,
        long close,

        long volume
) {
}
