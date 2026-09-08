package arile.toy.stocksystem.bffserver.trade.dto;

import java.time.Instant;

public record TradeHistoryItem(
        Long tradeId,
        Long orderId,
        String stockCode,
        TradeType tradeType,
        Integer tradePrice,
        Integer tradeQuantity,
        Instant executedAt
) {
}
