package arile.toy.stocksystem.stockserver.trading.event;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderDto;
import arile.toy.stocksystem.stockserver.trading.dto.order.OrderType;

public record StockServerOrderRequestEvent(
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity
) {
    public static StockServerOrderRequestEvent of(String username, String stockCode, OrderType orderType, Integer orderPrice, Integer orderQuantity) {
        return new StockServerOrderRequestEvent(username, stockCode, orderType, orderPrice, orderQuantity);
    }

    public static StockServerOrderRequestEvent fromAutoOrderDto(AutoOrderDto autoOrderDto) {
        return new StockServerOrderRequestEvent(
                autoOrderDto.username(),
                autoOrderDto.stockCode(),
                autoOrderDto.autoOrderType().toOrderType(),
        autoOrderDto.orderPrice(),
                autoOrderDto.orderQuantity()
        );
    }
}
