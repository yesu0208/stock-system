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
                // 추적 상태는 저장된 추적값(없으면 등록 시점 값)으로 복구
                entity.resolveCurrentBasePrice(),
                entity.resolveCurrentTriggerPrice(),
                // 예약 금액 기준은 항상 등록 시점 발동가
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
