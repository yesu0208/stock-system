package arile.toy.stocksystem.stockserver.autoorder.event;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;

import java.time.Instant;

public record AutoOrderResponseEvent(
        Long autoOrderId,
        String username,
        String stockCode,
        AutoOrderType autoOrderType,
        LeverageRatio leverageRatio,
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
                autoOrderResponseMessage.stockCode(), autoOrderResponseMessage.autoOrderType(), autoOrderResponseMessage.leverageRatio(),
                autoOrderResponseMessage.triggerPrice(), autoOrderResponseMessage.orderPrice(),
                autoOrderResponseMessage.orderQuantity(), autoOrderResponseMessage.orderTime(), success, resultCode);
    }

    public static AutoOrderResponseEvent of(Long autoOrderId, String username, String stockCode, AutoOrderType autoOrderType,
                                            LeverageRatio leverageRatio, Integer triggerPrice, Integer orderPrice, Integer orderQuantity, Instant orderTime,
                                            boolean success, AutoOrderResultCode resultCode) {
        return new AutoOrderResponseEvent(autoOrderId, username, stockCode, autoOrderType, leverageRatio, triggerPrice, orderPrice, orderQuantity, orderTime, success, resultCode);
    }
}
