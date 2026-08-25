package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record ForeignInstitutionTrade(
        String date,
        String closePrice,
        String diff,
        String rate,

        String volume,

        String institutionNetBuy,

        String foreignNetBuy,
        String foreignHoldings,
        String foreignRate
) {
}
