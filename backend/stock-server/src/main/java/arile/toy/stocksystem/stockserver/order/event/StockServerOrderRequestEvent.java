package arile.toy.stocksystem.stockserver.order.event;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;

public record StockServerOrderRequestEvent(
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio,
        OrderExecutionType orderExecutionType
) {
    public static StockServerOrderRequestEvent of(String username, String stockCode, OrderType orderType,
                                                  Integer orderPrice, Integer orderQuantity, LeverageRatio leverageRatio,
                                                  OrderExecutionType orderExecutionType) {
        return new StockServerOrderRequestEvent(username, stockCode, orderType, orderPrice, orderQuantity,
                leverageRatio, orderExecutionType);
    }

    // 자동주문/트레일링스탑/OTOCO는 트리거된 시점의 지정가 체결로 취급 — LIMIT 고정
    public static StockServerOrderRequestEvent fromAutoOrderDto(AutoOrderDto autoOrderDto) {
        return new StockServerOrderRequestEvent(
                autoOrderDto.username(),
                autoOrderDto.stockCode(),
                autoOrderDto.autoOrderType().toOrderType(),
                autoOrderDto.orderPrice(),
                autoOrderDto.orderQuantity(),
                autoOrderDto.leverageRatio(),
                OrderExecutionType.LIMIT
        );
    }

    public static StockServerOrderRequestEvent fromTrailingStopDto(TrailingStopDto trailingStopDto) {
        return new StockServerOrderRequestEvent(
                trailingStopDto.username(),
                trailingStopDto.stockCode(),
                trailingStopDto.trailingStopType().toOrderType(),
                trailingStopDto.triggerPrice(),
                trailingStopDto.orderQuantity(),
                trailingStopDto.leverageRatio(),
                OrderExecutionType.LIMIT
        );
    }

    public static StockServerOrderRequestEvent fromOtocoEntry(OtocoDto otocoDto) {
        return new StockServerOrderRequestEvent(
                otocoDto.username(),
                otocoDto.stockCode(),
                OrderType.BUY,
                otocoDto.entryTriggerPrice(),
                otocoDto.orderQuantity(),
                otocoDto.leverageRatio(),
                OrderExecutionType.LIMIT
        );
    }

    public static StockServerOrderRequestEvent fromOtocoExit(OtocoDto otocoDto, Integer exitPrice) {
        return new StockServerOrderRequestEvent(
                otocoDto.username(),
                otocoDto.stockCode(),
                OrderType.SELL,
                exitPrice,
                otocoDto.orderQuantity(),
                otocoDto.leverageRatio(),
                OrderExecutionType.LIMIT
        );
    }
}
