package arile.toy.stocksystem.bffserver.trailingstop.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

public record TrailingStopResponse(
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        LeverageRatio leverageRatio
) {
}
