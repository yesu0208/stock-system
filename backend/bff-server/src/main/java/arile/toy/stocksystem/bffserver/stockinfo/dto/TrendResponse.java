package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record TrendResponse(
        List<InvestorTrendDto> data,
        boolean hasNext
) {
}