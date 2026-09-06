package arile.toy.stocksystem.stockserver.trailingstop.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;

import java.time.Instant;

public record StockServerTrailingStopResponseMessage(
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
    public static StockServerTrailingStopResponseMessage fromDto(TrailingStopDto dto) {
        return new StockServerTrailingStopResponseMessage(
                dto.trailingStopId(), dto.username(), dto.stockCode(), dto.trailingStopType(),
                dto.leverageRatio(), dto.orderQuantity(), dto.stopPercent(), dto.basePrice(),
                dto.triggerPrice(), dto.orderTime()
        );
    }
}
