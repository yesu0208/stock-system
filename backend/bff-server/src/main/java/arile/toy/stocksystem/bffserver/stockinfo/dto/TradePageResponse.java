package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record TradePageResponse(
        List<ForeignInstitutionTrade> items,
        boolean hasNext
) {
}