package arile.toy.stocksystem.bffserver.otoco.event;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoExitMode;

public record OtocoRequestEvent(
        String username,
        String stockCode,
        OtocoEntryDirection entryDirection,
        Integer orderQuantity,
        Integer entryTriggerPrice,
        OtocoExitMode tpMode,
        Integer tpPrice,
        Double tpPct,
        OtocoExitMode slMode,
        Integer slPrice,
        Double slPct,
        LeverageRatio leverageRatio
) {
}
