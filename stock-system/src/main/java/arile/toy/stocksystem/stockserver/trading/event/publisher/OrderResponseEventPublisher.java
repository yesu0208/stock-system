package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.dto.order.OrderErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.order.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.trading.event.StockServerOrderRequestEvent;

public interface OrderResponseEventPublisher {
    void publish(StockServerOrderResponseMessage orderResponseMessage);
    void publishError(StockServerOrderRequestEvent orderRequestEvent, OrderErrorCode orderErrorCode);
}
