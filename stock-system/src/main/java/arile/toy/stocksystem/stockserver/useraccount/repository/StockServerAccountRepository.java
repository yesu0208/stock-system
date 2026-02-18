package arile.toy.stocksystem.stockserver.useraccount.repository;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;

import java.util.Map;

public interface StockServerAccountRepository {
    void save(String username, StockServerAccountMessage account);
    StockServerAccountMessage findByUsername(String username);
    Long getAvailableCash(String username);
    Long getReservedCash(String username);
    void updateAccountAfterClose(String username, Long availableCash);
    void saveStocks(String username, Map<String, StockInfo> stocks);
    Map<String, StockInfo> getStocks(String username);
}
