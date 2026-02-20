package arile.toy.stocksystem.stockserver.trade.event;

import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;

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
    public static TradeResponseEvent fromEntity(TradeEntity tradeEntity) {
        return new TradeResponseEvent(
                tradeEntity.getTradeId(),
                tradeEntity.getOrderId(),
                tradeEntity.getUsername(),
                tradeEntity.getStockCode(),
                tradeEntity.getTradeType(),
                tradeEntity.getTradePrice(),
                tradeEntity.getTradeQuantity(),
                tradeEntity.getExecutedAt()
        );
    }
}
