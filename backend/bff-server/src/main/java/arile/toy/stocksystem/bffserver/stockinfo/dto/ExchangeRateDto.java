package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record ExchangeRateDto(
        String currencyName,
        String currencyCode,
        String rate,
        String change,
        String direction,
        String time,
        String detailUrl
) {
}