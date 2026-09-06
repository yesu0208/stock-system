package arile.toy.stocksystem.bffserver.otococancel.event;

import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelErrorCode;

public record OtocoCancelResponseEvent(
        Long otocoId,
        String username,
        String stockCode,
        OtocoEntryDirection entryDirection,
        Integer entryTriggerPrice,
        Integer orderQuantity,
        boolean success,
        OtocoCancelErrorCode errorCode
) {
}
