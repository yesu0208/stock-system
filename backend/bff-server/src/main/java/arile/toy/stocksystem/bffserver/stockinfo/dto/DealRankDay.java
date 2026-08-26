package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record DealRankDay(
        String dealDate,
        List<DealRankItem> items
) {
}
