package arile.toy.stocksystem.stockserver.trailingstop.event;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;

public record StockServerTrailingStopRequestEvent(
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        Integer orderQuantity,
        Double stopPercent,
        Integer basePrice,
        LeverageRatio leverageRatio
) {
    public static StockServerTrailingStopRequestEvent of(String username, String stockCode, TrailingStopType trailingStopType,
                                                         Integer orderQuantity, Double stopPercent, Integer basePrice,
                                                         LeverageRatio leverageRatio) {
        return new StockServerTrailingStopRequestEvent(username, stockCode, trailingStopType, orderQuantity, stopPercent, basePrice, leverageRatio);
    }
}
