package arile.toy.stocksystem.bffserver.trailingstop.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record TrailingStopResultResponse(
        ResponseType responseType,
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
        String errorMessage
) {
    public static TrailingStopResultResponse of(ResponseType responseType, Long trailingStopId, String username, String stockCode,
                                                TrailingStopType trailingStopType, LeverageRatio leverageRatio,
                                                Integer orderQuantity, Double stopPercent, Integer basePrice,
                                                Integer triggerPrice, Instant orderTime, String errorMessage) {
        return new TrailingStopResultResponse(responseType, trailingStopId, username, stockCode, trailingStopType,
                leverageRatio, orderQuantity, stopPercent, basePrice, triggerPrice, orderTime, errorMessage);
    }
}
