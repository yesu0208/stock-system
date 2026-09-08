package arile.toy.stocksystem.stockserver.trade.dto;

import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;

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
    public static TradeHistoryItem fromEntity(TradeEntity entity) {
        return new TradeHistoryItem(
                entity.getTradeId(), entity.getOrderId(), entity.getStockCode(), entity.getTradeType(),
                entity.getTradePrice(), entity.getTradeQuantity(), entity.getExecutedAt()
        );
    }
}
