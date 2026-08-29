package arile.toy.stocksystem.stockserver.trade.event;

import arile.toy.stocksystem.stockserver.trade.dto.TradeType;

public record TradeExecutedEvent(
        Long tradeId,
        Long orderId,
        String username,
        String stockCode,
        TradeType tradeType,
        Integer orderPrice,
        Integer tradePrice,
        Integer tradeQuantity
) {
    public static TradeExecutedEvent of(Long tradeId, Long orderId, String username, String stockCode,
                                        TradeType tradeType, Integer orderPrice, Integer tradePrice, Integer tradeQuantity) {
        return new TradeExecutedEvent(tradeId, orderId, username, stockCode, tradeType, orderPrice, tradePrice, tradeQuantity);
    }
}
