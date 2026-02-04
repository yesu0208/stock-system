package arile.toy.stocksystem.stockserver.trading.repository;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.StockServerAutoOrderResponseMessage;

public interface StockServerAutoOrderResponseRepository {
    void save(StockServerAutoOrderResponseMessage stockServerAutoOrderResponseMessage);
    void delete(String username, Long autoOrderId);
}
