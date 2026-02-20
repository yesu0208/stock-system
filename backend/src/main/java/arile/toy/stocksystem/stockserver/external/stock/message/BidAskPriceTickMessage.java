package arile.toy.stocksystem.stockserver.external.stock.message;

import arile.toy.stocksystem.stockserver.external.stock.event.PriceLevel;

import java.util.List;

public record BidAskPriceTickMessage(
        TickMessageType tickMessageType,
        String stockCode,
        List<PriceLevel> asks,
        List<PriceLevel> bids,
        Integer totalAskNum,
        Integer totalBidNum
) {
}
