package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record UpjongInfo(
        String name,
        String no,
        String changeRate,
        String total,
        String rise,
        String steady,
        String fall,
        String graphRatio,
        String totalMarketCap,
        String totalTradingVolume,
        String totalTradingValue,
        List<UpjongRankItem> topByChangeRate,
        List<UpjongRankItem> topByMarketCap,
        List<UpjongRankItem> topByTradingValue
) {
}
