package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;

public interface StockServerStockSummaryRepository {
    void save(StockSummaryTickMessage message);
    StockSummaryTickMessage findByStockCode(String stockCode);
}
