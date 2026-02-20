package arile.toy.stocksystem.stockserver.order.event.publisher;

import arile.toy.stocksystem.stockserver.order.dto.OrderErrorCode;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;

public interface OrderResponseEventPublisher {
    void publish(StockServerOrderResponseMessage orderResponseMessage);
    void publishError(StockServerOrderRequestEvent orderRequestEvent, OrderErrorCode orderErrorCode);
}
