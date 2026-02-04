package arile.toy.stocksystem.stockserver.trading.event;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderErrorCode;
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
        AutoOrderErrorCode errorCode
) {
    public static AutoOrderResponseEvent fromAutoOrderResponseMessage(StockServerAutoOrderResponseMessage autoOrderResponseMessage,
                                                                      boolean success, AutoOrderErrorCode errorCode) {
        return new  AutoOrderResponseEvent(autoOrderResponseMessage.autoOrderId(), autoOrderResponseMessage.username(),
                autoOrderResponseMessage.stockCode(), autoOrderResponseMessage.autoOrderType(),
                autoOrderResponseMessage.triggerPrice(), autoOrderResponseMessage.orderPrice(),
                autoOrderResponseMessage.orderQuantity(), autoOrderResponseMessage.orderTime(), success, errorCode);
    }

    public static AutoOrderResponseEvent of(Long autoOrderId, String username, String stockCode, AutoOrderType autoOrderType,
                                        Integer triggerPrice, Integer orderPrice, Integer orderQuantity, Instant orderTime, boolean success, AutoOrderErrorCode errorCode) {
        return new AutoOrderResponseEvent(autoOrderId, username, stockCode, autoOrderType, triggerPrice, orderPrice, orderQuantity, orderTime, success, errorCode);
    }
}
