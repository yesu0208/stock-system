package arile.toy.stocksystem.bffserver.trailingstop.event;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResultCode;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;

import java.time.Instant;

public record TrailingStopResponseEvent(
        Long trailingStopId,
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        LeverageRatio leverageRatio,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        Integer triggerPrice,
        Instant orderTime,
        boolean success,
        TrailingStopResultCode resultCode
) {
}
