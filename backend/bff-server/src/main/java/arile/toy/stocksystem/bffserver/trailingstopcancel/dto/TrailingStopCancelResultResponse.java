package arile.toy.stocksystem.bffserver.trailingstopcancel.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;

public record TrailingStopCancelResultResponse(
        ResponseType responseType,
        Long trailingStopId,
        String username,
        String stockCode,
        TrailingStopType trailingStopType,
        Integer triggerPrice,
        Integer orderQuantity,
        String errorMessage
) {
    public static TrailingStopCancelResultResponse of(ResponseType responseType, Long trailingStopId, String username,
                                                      String stockCode, TrailingStopType trailingStopType,
                                                      Integer triggerPrice, Integer orderQuantity, String errorMessage) {
        return new TrailingStopCancelResultResponse(responseType, trailingStopId, username, stockCode,
                trailingStopType, triggerPrice, orderQuantity, errorMessage);
    }
}
