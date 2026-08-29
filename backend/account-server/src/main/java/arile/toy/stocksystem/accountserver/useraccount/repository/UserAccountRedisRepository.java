package arile.toy.stocksystem.accountserver.useraccount.repository;

import arile.toy.stocksystem.accountserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.accountserver.useraccount.dto.UserAccountMessage;

import java.util.Map;

public interface UserAccountRedisRepository {
    void save(String username, UserAccountMessage account);
    UserAccountMessage findByUsername(String username);
    Long getAvailableCash(String username);
    Long getReservedCash(String username);
    void updateAccountAfterClose(String username, Long availableCash);
    void saveStocks(String username, Map<String, StockInfo> stocks);
    Map<String, StockInfo> getStocks(String username);
}
