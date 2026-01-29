package arile.toy.stocksystem.bffserver.external.stock.message;

import arile.toy.stocksystem.bffserver.external.stock.event.PriceLevel;

import java.util.List;

public record BffServerBidAskPriceTickMessage(
        TickMessageType tickMessageType,
        String stockCode,
        List<PriceLevel> asks,
        List<PriceLevel> bids,
        Integer totalAskNum,
        Integer totalBidNum
) {
}
