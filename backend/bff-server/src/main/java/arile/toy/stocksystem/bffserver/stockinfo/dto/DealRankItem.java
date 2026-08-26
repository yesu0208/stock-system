package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.math.BigDecimal;

public record DealRankItem(
        int rank,
        String stockCode,
        String stockName,
        BigDecimal quantity,
        BigDecimal amount,
        BigDecimal volume
) {
}
