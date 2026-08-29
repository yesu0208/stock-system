package arile.toy.stocksystem.stockserver.trade.dto;

import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;

public record TradeResult (
    TradeEntity tradeEntity
){
    public static TradeResult of(TradeEntity tradeEntity){
        return new TradeResult(tradeEntity);
    }
}
