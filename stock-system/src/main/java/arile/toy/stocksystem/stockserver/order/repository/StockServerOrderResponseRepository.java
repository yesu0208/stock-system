package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;

public interface StockServerOrderResponseRepository {
    void save(StockServerOrderResponseMessage stockServerOrderResponseMessage);
    void delete(String username, Long orderId);
    void update(String username, Long orderId, StockServerOrderResponseMessage newValue);
}
