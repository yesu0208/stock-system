package arile.toy.stocksystem.bffserver.trailingstop.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record TrailingStopResponseMessage(
        Long trailingStopId,
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        LeverageRatio leverageRatio,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        Integer triggerPrice,
        Instant orderTime
) {
}
