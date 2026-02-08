package arile.toy.stocksystem.stockserver.trading.event;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderResultCode;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderType;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.StockServerAutoOrderResponseMessage;

import java.time.Instant;

public record AutoOrderResponseEvent(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        Integer triggerPrice,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        boolean success,
        AutoOrderResultCode resultCode
) {
    public static AutoOrderResponseEvent fromAutoOrderResponseMessage(StockServerAutoOrderResponseMessage autoOrderResponseMessage,
                                                                      boolean success, AutoOrderResultCode resultCode) {
        return new  AutoOrderResponseEvent(autoOrderResponseMessage.autoOrderId(), autoOrderResponseMessage.username(),
                autoOrderResponseMessage.stockCode(), autoOrderResponseMessage.autoOrderType(),
                autoOrderResponseMessage.triggerPrice(), autoOrderResponseMessage.orderPrice(),
                autoOrderResponseMessage.orderQuantity(), autoOrderResponseMessage.orderTime(), success, resultCode);
    }

    public static AutoOrderResponseEvent of(Long autoOrderId, String username, String stockCode, AutoOrderType autoOrderType,
                                        Integer triggerPrice, Integer orderPrice, Integer orderQuantity, Instant orderTime, boolean success, AutoOrderResultCode resultCode) {
        return new AutoOrderResponseEvent(autoOrderId, username, stockCode, autoOrderType, triggerPrice, orderPrice, orderQuantity, orderTime, success, resultCode);
    }
}
