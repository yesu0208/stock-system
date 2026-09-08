package arile.toy.stocksystem.bffserver.portfolio.dto;

import java.util.List;

public record PortfolioSectorItem(
        String sector,
        Long evaluationAmount,
        /** 전체 자산 대비 이 업종의 비중 (%) */
        Double ratioInTotal,
        List<PortfolioStockItem> stocks
) {
}
