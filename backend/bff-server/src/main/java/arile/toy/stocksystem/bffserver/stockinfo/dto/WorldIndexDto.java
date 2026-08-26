package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record WorldIndexDto(
        String name,
        String currentPrice,
        String diffPrice,
        String diffRate,
        String direction,
        String dateTime,
        String detailUrl
) {
}