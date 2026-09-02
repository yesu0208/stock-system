package arile.toy.stocksystem.bffserver.order.dto;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;

import java.time.Instant;

public record OrderResultResponse(
        ResponseType responseType,
        Long orderId,
        String username,
        String stockCode,
        OrderType orderType,
        LeverageRatio leverageRatio,
        Integer orderPrice,
        Integer orderQuantity,
        Instant orderTime,
        String errorMessage
) {
    public static OrderResultResponse of(ResponseType responseType, Long orderId, String username, String stockCode,
                                         OrderType orderType, LeverageRatio leverageRatio, Integer orderPrice,
                                         Integer orderQuantity, Instant orderTime, String errorMessage) {
        return new OrderResultResponse(responseType, orderId, username, stockCode, orderType, leverageRatio,
                orderPrice, orderQuantity, orderTime, errorMessage);
    }
}