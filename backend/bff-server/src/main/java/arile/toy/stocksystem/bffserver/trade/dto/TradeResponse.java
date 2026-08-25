package arile.toy.stocksystem.bffserver.trade.dto;

import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;

import java.time.Instant;

public record TradeResponse(
        Long tradeId,
        Long orderId,
        String username,
        String stockCode,
        TradeType tradeType,
        Integer tradePrice,
        Integer tradeQuantity,
        Instant executedAt
) {
    public static TradeResponse fromEvent(TradeResponseEvent tradeResponseEvent) {
        return new TradeResponse(tradeResponseEvent.tradeId(),
                tradeResponseEvent.orderId(),
                tradeResponseEvent.username(),
                tradeResponseEvent.stockCode(),
                tradeResponseEvent.tradeType(),
                tradeResponseEvent.tradePrice(),
                tradeResponseEvent.tradeQuantity(),
                tradeResponseEvent.executedAt());
    }
}
