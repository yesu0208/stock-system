package arile.toy.stocksystem.stockserver.otoco.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;

import java.time.Instant;

public record OtocoDto(
        Long otocoId,
        String username,
        String stockCode,
        OtocoEntryDirection entryDirection,
        LeverageRatio leverageRatio,
        Integer orderQuantity,
        Integer entryTriggerPrice,
        Integer tpTriggerPrice,
        Integer slTriggerPrice,
        Long entryOrderId,
        OtocoStatus otocoStatus,
        Instant orderTime
) {
    public static OtocoDto fromEntity(OtocoEntity entity) {
        return new OtocoDto(
                entity.getOtocoId(), entity.getUsername(), entity.getStockCode(), entity.getEntryDirection(),
                entity.getLeverageRatio(), entity.getOrderQuantity(), entity.getEntryTriggerPrice(),
                entity.getTpTriggerPrice(), entity.getSlTriggerPrice(), entity.getEntryOrderId(),
                entity.getOtocoStatus(), entity.getOrderTime()
        );
    }
}
