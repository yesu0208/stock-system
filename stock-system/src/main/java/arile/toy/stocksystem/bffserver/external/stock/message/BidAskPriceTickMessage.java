package arile.toy.stocksystem.bffserver.external.stock.message;

import arile.toy.stocksystem.bffserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.bffserver.external.stock.event.PriceLevel;

import java.util.List;

public record BidAskPriceTickMessage(
        TickMessageType tickMessageType,
        String stockCode,
        List<PriceLevel> asks,
        List<PriceLevel> bids,
        Integer totalAskNum,
        Integer totalBidNum
) {
    public static BidAskPriceTickMessage fromEvent(BidAskPriceTickEvent event) {
        return new BidAskPriceTickMessage(
                TickMessageType.BIDASKPRICE,
                event.stockCode(),
                event.asks() == null ? List.of() : List.copyOf(event.asks()),
                event.bids() == null ? List.of() : List.copyOf(event.bids()),
                event.totalAskNum(),
                event.totalBidNum()
        );
    }
}
