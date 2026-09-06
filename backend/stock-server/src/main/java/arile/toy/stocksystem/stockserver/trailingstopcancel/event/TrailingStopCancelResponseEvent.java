package arile.toy.stocksystem.stockserver.trailingstopcancel.event;

import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstopcancel.dto.TrailingStopCancelErrorCode;

public record TrailingStopCancelResponseEvent(
        Long trailingStopId,
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        Integer triggerPrice,
        Integer orderQuantity,
        boolean success,
        TrailingStopCancelErrorCode errorCode
) {
    public static TrailingStopCancelResponseEvent of(TrailingStopEntity entity, boolean success, TrailingStopCancelErrorCode errorCode) {
        return new TrailingStopCancelResponseEvent(
                entity.getTrailingStopId(),
                entity.getUsername(),
                entity.getStockCode(),
                entity.getTrailingStopType(),
                entity.getTriggerPrice(),
                entity.getOrderQuantity(),
                success,
                errorCode
        );
    }
}
