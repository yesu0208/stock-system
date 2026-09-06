package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;

import java.time.Instant;

public record TrailingStopDto(
        Long trailingStopId,
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        LeverageRatio leverageRatio,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        Integer triggerPrice,
        Integer initialTriggerPrice,
        TrailingStopStatus trailingStopStatus,
        Instant orderTime
) {
    public static TrailingStopDto fromEntity(TrailingStopEntity entity) {
        return new TrailingStopDto(
                entity.getTrailingStopId(),
                entity.getUsername(),
                entity.getStockCode(),
                entity.getTrailingStopType(),
                entity.getLeverageRatio(),
                entity.getOrderQuantity(),
                entity.getStopPercent(),
                entity.getBasePrice(),
                entity.getTriggerPrice(),
                entity.getTriggerPrice(),
                entity.getTrailingStopStatus(),
                entity.getOrderTime()
        );
    }

    public TrailingStopDto withUpdatedTrail(Integer newBasePrice, Integer newTriggerPrice) {
        return new TrailingStopDto(trailingStopId, username, stockCode, trailingStopType, leverageRatio,
                orderQuantity, stopPercent, newBasePrice, newTriggerPrice, initialTriggerPrice,
                trailingStopStatus, orderTime);
    }
}
