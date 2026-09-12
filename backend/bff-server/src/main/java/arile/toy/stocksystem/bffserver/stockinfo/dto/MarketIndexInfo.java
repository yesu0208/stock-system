package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record MarketIndexInfo(
        String name,
        String currentIndex,
        String changeValue,
        String changeRate,
        String direction,
        String baseTime,
        String prevClose,
        String openPrice,
        String highPrice,
        String lowPrice,
        String high52Weeks,
        String low52Weeks,
        String volume,
        String tradingValue,
        MarketBreadth breadth,
        ProgramTrade programTrade,
        InvestorTrend investorTrend
) {
}
