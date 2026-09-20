package arile.toy.stocksystem.stockserver.order.event;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoLeg;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;

public record StockServerOrderRequestEvent(
        String username,
        String stockCode,
        OrderType orderType,
        Integer orderPrice,
        Integer orderQuantity,
        LeverageRatio leverageRatio,
        OrderExecutionType orderExecutionType,
        OrderOrigin origin,
        Long originId // autoOrderId / otocoId / trailingStopId. MANUAL이면 null
) {
    public static StockServerOrderRequestEvent of(String username, String stockCode, OrderType orderType,
                                                  Integer orderPrice, Integer orderQuantity, LeverageRatio leverageRatio,
                                                  OrderExecutionType orderExecutionType) {
        return new StockServerOrderRequestEvent(username, stockCode, orderType, orderPrice, orderQuantity,
                leverageRatio, orderExecutionType, OrderOrigin.MANUAL, null);
    }

    public static StockServerOrderRequestEvent fromAutoOrderDto(AutoOrderDto autoOrderDto) {
        return new StockServerOrderRequestEvent(
                autoOrderDto.username(),
                autoOrderDto.stockCode(),
                autoOrderDto.autoOrderType().toOrderType(),
                autoOrderDto.orderPrice(),
                autoOrderDto.orderQuantity(),
                autoOrderDto.leverageRatio(),
                OrderExecutionType.LIMIT,
                OrderOrigin.AUTO_ORDER,
                autoOrderDto.autoOrderId()
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
                OrderExecutionType.LIMIT,
                OrderOrigin.TRAILING_STOP,
                trailingStopDto.trailingStopId()
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
                OrderExecutionType.LIMIT,
                OrderOrigin.OTOCO_ENTRY,
                otocoDto.otocoId()
        );
    }

    // OtocoLeg를 파라미터로 받아 익절/손절 구분
    public static StockServerOrderRequestEvent fromOtocoExit(OtocoDto otocoDto, Integer exitPrice, OtocoLeg leg) {
        OrderOrigin origin = leg == OtocoLeg.TAKE_PROFIT ? OrderOrigin.OTOCO_TAKE_PROFIT : OrderOrigin.OTOCO_STOP_LOSS;

        return new StockServerOrderRequestEvent(
                otocoDto.username(),
                otocoDto.stockCode(),
                OrderType.SELL,
                exitPrice,
                otocoDto.orderQuantity(),
                otocoDto.leverageRatio(),
                OrderExecutionType.LIMIT,
                origin,
                otocoDto.otocoId()
        );
    }
}
