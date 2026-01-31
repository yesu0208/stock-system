package arile.toy.stocksystem.stockserver.trading.repository;

import arile.toy.stocksystem.stockserver.trading.dto.order.StockServerOrderResponseMessage;

public interface StockServerOrderResponseRepository {
    void save(StockServerOrderResponseMessage stockServerOrderResponseMessage);
    void delete(String username, Long orderId);
    void update(String username, Long orderId, StockServerOrderResponseMessage newValue);
}
