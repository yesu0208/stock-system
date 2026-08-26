package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record GlobalMarketResponse(
        List<ExchangeRateDto> exchangeRates,
        List<WorldIndexDto> worldIndexes
) {
}