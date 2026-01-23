package arile.toy.stocksystem.stockserver.external.stock.event;

import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;

import java.util.List;

public record BidAskPriceTickEvent(
        TickMessageType tickMessageType,
        String stockCode,
        List<PriceLevel> asks,
        List<PriceLevel> bids,
        Integer totalAskNum,
        Integer totalBidNum
) {
    public static BidAskPriceTickEvent fromMessage(BidAskPriceTickMessage message) {
        return new BidAskPriceTickEvent(
                message.tickMessageType(),
                message.stockCode(),
                message.asks() == null ? List.of() : List.copyOf(message.asks()),
                message.bids() == null ? List.of() : List.copyOf(message.bids()),
                message.totalAskNum(),
                message.totalBidNum()
        );
    }
}