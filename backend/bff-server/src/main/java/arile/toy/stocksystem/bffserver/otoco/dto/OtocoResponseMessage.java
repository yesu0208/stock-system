package arile.toy.stocksystem.bffserver.otoco.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record OtocoResponseMessage(
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
        Instant orderTime
) {
}
