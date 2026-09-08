package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;

import java.time.Instant;

public record TrailingStopHistoryItem(
        Long trailingStopId,
        String stockCode,
        TrailingStopType trailingStopType,
        LeverageRatio leverageRatio,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        Integer triggerPrice,
        TrailingStopStatus trailingStopStatus,
        Instant orderTime
) {
    public static TrailingStopHistoryItem fromEntity(TrailingStopEntity entity) {
        return new TrailingStopHistoryItem(
                entity.getTrailingStopId(), entity.getStockCode(), entity.getTrailingStopType(), entity.getLeverageRatio(),
                entity.getOrderQuantity(), entity.getStopPercent(), entity.getBasePrice(), entity.getTriggerPrice(),
                entity.getTrailingStopStatus(), entity.getOrderTime()
        );
    }
}
