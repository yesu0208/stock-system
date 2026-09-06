package arile.toy.stocksystem.bffserver.otococancel.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;

public record OtocoCancelResultResponse(
        ResponseType responseType,
        Long otocoId,
        String username,
        String stockCode,
        OtocoEntryDirection entryDirection,
        Integer entryTriggerPrice,
        Integer orderQuantity,
        String errorMessage
) {
    public static OtocoCancelResultResponse of(ResponseType responseType, Long otocoId, String username, String stockCode,
                                               OtocoEntryDirection entryDirection, Integer entryTriggerPrice,
                                               Integer orderQuantity, String errorMessage) {
        return new OtocoCancelResultResponse(responseType, otocoId, username, stockCode, entryDirection,
                entryTriggerPrice, orderQuantity, errorMessage);
    }
}
