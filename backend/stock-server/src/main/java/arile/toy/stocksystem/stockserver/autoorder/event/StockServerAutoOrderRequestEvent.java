package arile.toy.stocksystem.stockserver.autoorder.event;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;

public record StockServerAutoOrderRequestEvent(
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio
) {
    public static StockServerAutoOrderRequestEvent of(
            String username, String stockCode,AutoOrderType autoOrderType,
            Integer triggerPrice, Integer orderPrice, Integer orderQuantity, LeverageRatio leverageRatio) {

        return new StockServerAutoOrderRequestEvent(
                username, stockCode, autoOrderType, triggerPrice, orderPrice, orderQuantity,  leverageRatio);
    }
}
