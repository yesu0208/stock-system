package arile.toy.stocksystem.stockserver.autoorder.repository;

import arile.toy.stocksystem.stockserver.autoorder.dto.StockServerAutoOrderResponseMessage;

public interface StockServerAutoOrderResponseRepository {
    void save(StockServerAutoOrderResponseMessage stockServerAutoOrderResponseMessage);
    void delete(String username, Long autoOrderId);
}
