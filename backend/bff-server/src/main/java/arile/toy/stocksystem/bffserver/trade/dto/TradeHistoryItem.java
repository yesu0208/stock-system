package arile.toy.stocksystem.bffserver.trade.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.order.dto.OrderOrigin;

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
}
