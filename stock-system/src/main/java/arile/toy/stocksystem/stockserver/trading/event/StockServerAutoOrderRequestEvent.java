package arile.toy.stocksystem.stockserver.trading.event;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderType;

public record StockServerAutoOrderRequestEvent(
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity
) {
    public static StockServerAutoOrderRequestEvent of(String username, String stockCode, AutoOrderType autoOrderType, Integer triggerPrice, Integer orderPrice, Integer orderQuantity) {
        return new StockServerAutoOrderRequestEvent(username, stockCode, autoOrderType, triggerPrice, orderPrice, orderQuantity);
    }
}
