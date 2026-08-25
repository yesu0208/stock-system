package arile.toy.stocksystem.bffserver.order.event.publisher;

import arile.toy.stocksystem.bffserver.order.event.OrderRequestEvent;

public interface OrderRequestEventPublisher {
    void publishOrder(OrderRequestEvent event);
}
