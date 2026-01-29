package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;

public interface StockServerBidAskPriceRepository {
    void save(BidAskPriceTickMessage message);
}
