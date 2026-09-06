package arile.toy.stocksystem.bffserver.otoco.event;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoStatus;

import java.time.Instant;

public record OtocoResponseEvent(
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
        Instant orderTime,
        boolean success,
        OtocoResultCode resultCode
) {
}
