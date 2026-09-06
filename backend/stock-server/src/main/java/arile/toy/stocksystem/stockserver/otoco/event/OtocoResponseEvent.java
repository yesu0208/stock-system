package arile.toy.stocksystem.stockserver.otoco.event;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.*;

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
    public static OtocoResponseEvent fromMessage(StockServerOtocoResponseMessage m, boolean success, OtocoResultCode resultCode) {
        return new OtocoResponseEvent(m.otocoId(), m.username(), m.stockCode(), m.entryDirection(), m.leverageRatio(),
                m.orderQuantity(), m.entryTriggerPrice(), m.tpTriggerPrice(), m.slTriggerPrice(), m.otocoStatus(),
                m.orderTime(), success, resultCode);
    }

    public static OtocoResponseEvent of(Long otocoId, String username, String stockCode, OtocoEntryDirection entryDirection,
                                        LeverageRatio leverageRatio, Integer orderQuantity, Integer entryTriggerPrice,
                                        Integer tpTriggerPrice, Integer slTriggerPrice, OtocoStatus otocoStatus,
                                        Instant orderTime, boolean success, OtocoResultCode resultCode) {
        return new OtocoResponseEvent(otocoId, username, stockCode, entryDirection, leverageRatio, orderQuantity,
                entryTriggerPrice, tpTriggerPrice, slTriggerPrice, otocoStatus, orderTime, success, resultCode);
    }
}
