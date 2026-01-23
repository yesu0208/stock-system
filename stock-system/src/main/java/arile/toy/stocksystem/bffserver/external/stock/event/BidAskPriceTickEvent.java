package arile.toy.stocksystem.bffserver.external.stock.event;

import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;

import java.util.List;

public record BidAskPriceTickEvent(
        TickMessageType tickMessageType,
        String stockCode,
        List<PriceLevel> asks,
        List<PriceLevel> bids,
        Integer totalAskNum,
        Integer totalBidNum
) {
}