package arile.toy.stocksystem.bffserver.trade.event;


import arile.toy.stocksystem.bffserver.trade.dto.TradeType;

import java.time.Instant;

public record TradeResponseEvent(
        Long tradeId,
        Long orderId,
        String username,
        String stockCode,
        TradeType tradeType,
        Integer tradePrice,
        Integer tradeQuantity,
        Instant executedAt
) {
}
