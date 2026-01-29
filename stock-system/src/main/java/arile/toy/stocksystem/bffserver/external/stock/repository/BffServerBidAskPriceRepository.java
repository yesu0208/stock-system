package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;

public interface BffServerBidAskPriceRepository {
    BffServerBidAskPriceTickMessage findByStockCode(String stockCode);
}
