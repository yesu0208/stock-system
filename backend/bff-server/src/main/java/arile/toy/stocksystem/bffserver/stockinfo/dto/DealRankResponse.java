package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record DealRankResponse(
        String market,
        String investorType,
        String dealType,
        List<DealRankDay> days
) {
}
