package arile.toy.stocksystem.stockserver.trade.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;

import java.time.Instant;

public record TradeHistoryItem(
        Long tradeId,
        Long orderId,
        String stockCode,
        TradeType tradeType,
        Integer tradePrice,
        Integer tradeQuantity,
        Instant executedAt,
        LeverageRatio leverageRatio,
        Long notionalValue,
        Long initialMargin,
        Double maintenanceMarginRate,
        Long liquidationPrice,
        OrderOrigin origin,
        Long originId
) {
    private static final double MAINTENANCE_RATIO = 1.4;

    public static TradeHistoryItem fromEntity(TradeEntity entity) {
        long notionalValue = (long) entity.getTradePrice() * entity.getTradeQuantity();

        Long initialMargin = null;
        Double maintenanceMarginRate = null;
        Long liquidationPrice = null;

        LeverageRatio leverageRatio = entity.getLeverageRatio();
        if (leverageRatio != null && !leverageRatio.isSpot()) {
            long margin = leverageRatio.calculateMarginDeposit(notionalValue);
            long loanAmount = leverageRatio.calculateLoanAmount(notionalValue);

            initialMargin = margin;
            maintenanceMarginRate = MAINTENANCE_RATIO;
            liquidationPrice = entity.getTradeQuantity() > 0
                    ? Math.round((MAINTENANCE_RATIO * loanAmount) / entity.getTradeQuantity())
                    : 0L;
        }

        return new TradeHistoryItem(
                entity.getTradeId(), entity.getOrderId(), entity.getStockCode(), entity.getTradeType(),
                entity.getTradePrice(), entity.getTradeQuantity(), entity.getExecutedAt(),
                leverageRatio, notionalValue, initialMargin, maintenanceMarginRate, liquidationPrice,
                entity.getOrigin(),
                entity.getOriginId()
        );
    }
}
