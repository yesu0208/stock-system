package arile.toy.stocksystem.stockserver.order.event.publisher;

import arile.toy.stocksystem.stockserver.order.event.QueuePositionEvent;

public interface QueuePositionEventPublisher {
    void publish(QueuePositionEvent event);
}
