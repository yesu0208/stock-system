package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record MarketIndexInfo(
        String name,
        String currentIndex,
        String changeValue,
        String changeRate,
        String direction,
        String baseTime,
        MarketBreadth breadth,
        ProgramTrade programTrade,
        InvestorTrend investorTrend
) {
}
