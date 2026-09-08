package arile.toy.stocksystem.bffserver.portfolio.dto;

import java.util.List;

public record PortfolioResponse(
        String username,
        /** 현금 + 현물 평가금액 + 레버리지 평가금액(gross) 합계 (부채 반영 x) */
        Long totalAssetValue,
        Long cashValue,
        /** 전체 자산 대비 현금 비중 (%) */
        Double cashRatio,
        List<PortfolioSectorItem> sectors
) {
}
