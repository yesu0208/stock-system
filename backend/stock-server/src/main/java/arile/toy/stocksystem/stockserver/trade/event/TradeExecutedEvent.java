package arile.toy.stocksystem.stockserver.trade.event;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trade.dto.TradeType;

public record TradeExecutedEvent(
        Long tradeId,
        Long orderId,
        String username,
        String stockCode,
        TradeType tradeType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer tradePrice,
        Integer tradeQuantity,
        Long reservedFeeConsumed,
        Long reservedMarginConsumed
) {
    public static TradeExecutedEvent of(Long tradeId, Long orderId, String username, String stockCode,
                                        TradeType tradeType, LeverageRatio leverageRatio,
                                        Integer orderPrice, Integer tradePrice, Integer tradeQuantity,
                                        Long reservedFeeConsumed, Long reservedMarginConsumed) {
        return new TradeExecutedEvent(tradeId, orderId, username, stockCode, tradeType, leverageRatio,
                orderPrice, tradePrice, tradeQuantity, reservedFeeConsumed, reservedMarginConsumed);
    }
}
