package arile.toy.stocksystem.stockserver.useraccount.repository;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;

public interface StockServerAccountRepository {
    void save(String username, StockServerAccountMessage account);
    StockServerAccountMessage findByUsername(String username);
}
