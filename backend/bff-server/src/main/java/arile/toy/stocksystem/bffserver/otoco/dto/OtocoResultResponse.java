package arile.toy.stocksystem.bffserver.otoco.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record OtocoResultResponse(
        ResponseType responseType,
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
        String errorMessage
) {
    public static OtocoResultResponse of(ResponseType responseType, Long otocoId, String username, String stockCode,
                                         OtocoEntryDirection entryDirection, LeverageRatio leverageRatio,
                                         Integer orderQuantity, Integer entryTriggerPrice, Integer tpTriggerPrice,
                                         Integer slTriggerPrice, OtocoStatus otocoStatus, Instant orderTime, String errorMessage) {
        return new OtocoResultResponse(responseType, otocoId, username, stockCode, entryDirection, leverageRatio,
                orderQuantity, entryTriggerPrice, tpTriggerPrice, slTriggerPrice, otocoStatus, orderTime, errorMessage);
    }
}
