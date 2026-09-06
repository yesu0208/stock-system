package arile.toy.stocksystem.bffserver.trailingstopcancel.event;

import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelErrorCode;

public record TrailingStopCancelResponseEvent(
        Long trailingStopId,
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        Integer triggerPrice,
        Integer orderQuantity,
        boolean success,
        TrailingStopCancelErrorCode errorCode
) {
}
