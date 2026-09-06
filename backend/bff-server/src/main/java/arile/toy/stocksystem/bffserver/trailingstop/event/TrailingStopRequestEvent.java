package arile.toy.stocksystem.bffserver.trailingstop.event;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;

public record TrailingStopRequestEvent(
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        LeverageRatio leverageRatio
) {
}
