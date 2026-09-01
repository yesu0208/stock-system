package arile.toy.stocksystem.stockserver.order.event;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;

public record StockServerOrderRequestEvent(
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio
) {
    public static StockServerOrderRequestEvent of(String username, String stockCode, OrderType orderType,
                                                  Integer orderPrice, Integer orderQuantity, LeverageRatio leverageRatio) {
        return new StockServerOrderRequestEvent(username, stockCode, orderType, orderPrice, orderQuantity, leverageRatio);
    }

    public static StockServerOrderRequestEvent fromAutoOrderDto(AutoOrderDto autoOrderDto) {
        return new StockServerOrderRequestEvent(
                autoOrderDto.username(),
                autoOrderDto.stockCode(),
                autoOrderDto.autoOrderType().toOrderType(),
                autoOrderDto.orderPrice(),
                autoOrderDto.orderQuantity(),
                autoOrderDto.leverageRatio()
        );
    }
}