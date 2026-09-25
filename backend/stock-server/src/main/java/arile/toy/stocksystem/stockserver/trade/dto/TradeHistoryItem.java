package arile.toy.stocksystem.stockserver.trade.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageMetrics;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;

import java.time.Instant;

public record TradeHistoryItem(
        Long tradeId,
        Long orderId,
        String stockCode,
        TradeType tradeType,
        Integer tradePrice,
        Integer tradeQuantity,
        Instant executedAt,
        LeverageRatio leverageRatio,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice,
        OrderOrigin origin,
        Long originId
) {
    public static TradeHistoryItem fromEntity(TradeEntity entity) {
        LeverageRatio leverageRatio = entity.getLeverageRatio();
        LeverageMetrics metrics = LeverageMetrics.of(
                leverageRatio, entity.getTradePrice(), entity.getTradeQuantity());

        return new TradeHistoryItem(
                entity.getTradeId(), entity.getOrderId(), entity.getStockCode(), entity.getTradeType(),
                entity.getTradePrice(), entity.getTradeQuantity(), entity.getExecutedAt(),
                leverageRatio, metrics.notionalValue(), metrics.initialMargin(),
                metrics.maintenanceMarginRate(), metrics.liquidationPrice(),
                entity.getOrigin(),
                entity.getOriginId()
        );
    }
}
