package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;

import java.util.List;

public interface BffServerStockSummaryRepository {
    BffServerStockSummaryTickMessage findByStockCode(String stockCode);
    List<BffServerStockSummaryTickMessage> findAll();
}
