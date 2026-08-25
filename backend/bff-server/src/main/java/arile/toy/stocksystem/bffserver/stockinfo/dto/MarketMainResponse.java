package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record MarketMainResponse(
        MarketIndexInfo kospi,
        MarketIndexInfo kosdaq,
        MarketIndexInfo kospi200
) {
}
