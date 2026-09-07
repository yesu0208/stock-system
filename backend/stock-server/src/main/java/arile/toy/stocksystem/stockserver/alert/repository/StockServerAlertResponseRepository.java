package arile.toy.stocksystem.stockserver.alert.repository;

import arile.toy.stocksystem.stockserver.alert.dto.StockServerAlertResponseMessage;

public interface StockServerAlertResponseRepository {
    void save(StockServerAlertResponseMessage stockServerAlertResponseMessage);
    void delete(String username, Long alertId);
}
