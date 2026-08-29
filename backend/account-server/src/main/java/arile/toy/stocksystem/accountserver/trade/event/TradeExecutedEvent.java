package arile.toy.stocksystem.accountserver.trade.event;

import arile.toy.stocksystem.accountserver.trade.dto.TradeType;

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
}
