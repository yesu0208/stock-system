package arile.toy.stocksystem.stockserver.otoco.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;

import java.time.Instant;

public record StockServerOtocoResponseMessage(
        Long otocoId,
        String username,
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
    public static StockServerOtocoResponseMessage fromEntity(
            arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity entity) {
        return new StockServerOtocoResponseMessage(
                entity.getOtocoId(), entity.getUsername(), entity.getStockCode(), entity.getEntryDirection(),
                entity.getLeverageRatio(), entity.getOrderQuantity(), entity.getEntryTriggerPrice(),
                entity.getTpTriggerPrice(), entity.getSlTriggerPrice(), entity.getOtocoStatus(),
                entity.getCompletedLeg(), entity.getOrderTime()
        );
    }
}
