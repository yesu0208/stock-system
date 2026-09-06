package arile.toy.stocksystem.stockserver.otoco.event;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoExitMode;

public record StockServerOtocoRequestEvent(
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
