package arile.toy.stocksystem.stockserver.order.event;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;

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

    public static StockServerOrderRequestEvent fromTrailingStopDto(TrailingStopDto trailingStopDto) {
        return new StockServerOrderRequestEvent(
                trailingStopDto.username(),
                trailingStopDto.stockCode(),
                trailingStopDto.trailingStopType().toOrderType(),
                trailingStopDto.triggerPrice(),
                trailingStopDto.orderQuantity(),
                trailingStopDto.leverageRatio()
        );
    }

    public static StockServerOrderRequestEvent fromOtocoEntry(OtocoDto otocoDto) {
        return new StockServerOrderRequestEvent(
                otocoDto.username(),
                otocoDto.stockCode(),
                OrderType.BUY,
                otocoDto.entryTriggerPrice(),
                otocoDto.orderQuantity(),
                otocoDto.leverageRatio()
        );
    }

    public static StockServerOrderRequestEvent fromOtocoExit(OtocoDto otocoDto, Integer exitPrice) {
        return new StockServerOrderRequestEvent(
                otocoDto.username(),
                otocoDto.stockCode(),
                OrderType.SELL,
                exitPrice,
                otocoDto.orderQuantity(),
                otocoDto.leverageRatio()
        );
    }
}
