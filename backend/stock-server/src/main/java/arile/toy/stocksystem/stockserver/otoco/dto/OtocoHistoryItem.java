package arile.toy.stocksystem.stockserver.otoco.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;

import java.time.Instant;

public record OtocoHistoryItem(
        Long otocoId,
        String stockCode,
        OtocoEntryDirection entryDirection,
        LeverageRatio leverageRatio,
        Integer orderQuantity,
        Integer entryTriggerPrice,
        Integer tpTriggerPrice,
        Integer slTriggerPrice,
        OtocoStatus otocoStatus,
        OtocoLeg completedLeg,
        Instant orderTime
) {
    public static OtocoHistoryItem fromEntity(OtocoEntity entity) {
        return new OtocoHistoryItem(
                entity.getOtocoId(), entity.getStockCode(), entity.getEntryDirection(), entity.getLeverageRatio(),
                entity.getOrderQuantity(), entity.getEntryTriggerPrice(), entity.getTpTriggerPrice(), entity.getSlTriggerPrice(),
                entity.getOtocoStatus(), entity.getCompletedLeg(), entity.getOrderTime()
        );
    }
}
