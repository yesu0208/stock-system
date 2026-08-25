package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record StockInfo(
        String marketCap,
        String marketCapRank,
        String listedShares,

        String parValue,
        String tradingUnit,

        String foreignLimit,
        String foreignOwned,
        String foreignRate,

        String opinion,
        String targetPrice,

        String high52,
        String low52,

        String per,
        String eps,

        String estimatedPer,
        String estimatedEps,

        String pbr,
        String bps,

        String dividendYield,

        String sameIndustryPer,
        String sameIndustryRate
) {
}
