package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;

public interface StockServerTradePriceRepository {
    void save(TradePriceTickMessage message);
}
