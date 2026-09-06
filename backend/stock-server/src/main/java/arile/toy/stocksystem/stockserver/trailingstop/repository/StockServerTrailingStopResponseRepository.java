package arile.toy.stocksystem.stockserver.trailingstop.repository;

import arile.toy.stocksystem.stockserver.trailingstop.dto.StockServerTrailingStopResponseMessage;

public interface StockServerTrailingStopResponseRepository {
    void save(StockServerTrailingStopResponseMessage message);
    void update(String username, Long trailingStopId, StockServerTrailingStopResponseMessage newValue);
    void delete(String username, Long trailingStopId);
}
