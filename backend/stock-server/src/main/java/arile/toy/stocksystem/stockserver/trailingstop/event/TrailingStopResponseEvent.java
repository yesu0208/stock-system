package arile.toy.stocksystem.stockserver.trailingstop.event;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopResultCode;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;

import java.time.Instant;

public record TrailingStopResponseEvent(
        Long trailingStopId,
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        LeverageRatio leverageRatio,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        Integer triggerPrice,
        Instant orderTime,
        boolean success,
        TrailingStopResultCode resultCode
) {
    public static TrailingStopResponseEvent fromResponseMessage(StockServerTrailingStopResponseMessage message,
                                                                boolean success, TrailingStopResultCode resultCode) {
        return new TrailingStopResponseEvent(message.trailingStopId(), message.username(), message.stockCode(),
                message.trailingStopType(), message.leverageRatio(), message.orderQuantity(), message.stopPercent(),
                message.basePrice(), message.triggerPrice(), message.orderTime(), success, resultCode);
    }

    public static TrailingStopResponseEvent of(Long trailingStopId, String username, String stockCode,
                                               TrailingStopType trailingStopType, LeverageRatio leverageRatio,
                                               Integer orderQuantity, Double stopPercent, Integer basePrice,
                                               Integer triggerPrice, Instant orderTime, boolean success,
                                               TrailingStopResultCode resultCode) {
        return new TrailingStopResponseEvent(trailingStopId, username, stockCode, trailingStopType, leverageRatio,
                orderQuantity, stopPercent, basePrice, triggerPrice, orderTime, success, resultCode);
    }
}
