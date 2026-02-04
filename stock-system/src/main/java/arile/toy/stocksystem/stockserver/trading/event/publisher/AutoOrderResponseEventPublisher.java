package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.StockServerAutoOrderResponseMessage;
import arile.toy.stocksystem.stockserver.trading.event.StockServerAutoOrderRequestEvent;

public interface AutoOrderResponseEventPublisher {
    void publish(StockServerAutoOrderResponseMessage orderResponseMessage);
    void publishError(StockServerAutoOrderRequestEvent orderRequestEvent, AutoOrderErrorCode orderErrorCode);
}
