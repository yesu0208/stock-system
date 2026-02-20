package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;

public interface BffServerTradePriceRepository {
    BffServerTradePriceTickMessage findByStockCode(String stockCode);
}
