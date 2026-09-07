package arile.toy.stocksystem.stockserver.alert.event.publisher;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDto;
import arile.toy.stocksystem.stockserver.alert.dto.AlertErrorCode;
import arile.toy.stocksystem.stockserver.alert.dto.StockServerAlertResponseMessage;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;

public interface AlertResponseEventPublisher {
    void publishRegistered(StockServerAlertResponseMessage message);
    void publishRegisterError(AlertRequestEvent request, AlertErrorCode errorCode);
    void publishFired(AlertDto alertDto, Integer currentPrice);
}
