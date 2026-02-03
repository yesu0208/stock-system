package arile.toy.stocksystem.stockserver.trading.dto.trade;

import arile.toy.stocksystem.stockserver.trading.entity.TradeEntity;

public record TradeResult (
    TradeEntity tradeEntity,
    Long totalAmount,
    Integer totalQuantity
){
    public static TradeResult of(TradeEntity tradeEntity, Long totalAmount, Integer totalQuantity){
        return new TradeResult(tradeEntity, totalAmount, totalQuantity);
    }
}
