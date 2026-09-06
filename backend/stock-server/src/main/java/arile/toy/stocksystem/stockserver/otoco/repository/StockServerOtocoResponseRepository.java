package arile.toy.stocksystem.stockserver.otoco.repository;

import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;

public interface StockServerOtocoResponseRepository {
    void save(StockServerOtocoResponseMessage message);
    void update(String username, Long otocoId, StockServerOtocoResponseMessage newValue);
    void delete(String username, Long otocoId);
}
