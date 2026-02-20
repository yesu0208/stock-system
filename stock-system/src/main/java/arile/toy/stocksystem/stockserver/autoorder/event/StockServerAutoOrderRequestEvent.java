package arile.toy.stocksystem.stockserver.autoorder.event;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;

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
