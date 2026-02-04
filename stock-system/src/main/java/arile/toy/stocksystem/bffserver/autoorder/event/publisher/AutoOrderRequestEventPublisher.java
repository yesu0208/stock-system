package arile.toy.stocksystem.bffserver.autoorder.event.publisher;

import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderRequestEvent;

public interface AutoOrderRequestEventPublisher {
    void publishAutoOrder(AutoOrderRequestEvent event);
}
