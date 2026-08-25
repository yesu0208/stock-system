package arile.toy.stocksystem.stockserver.cancel.event.publisher;

import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;

public interface CancelResponseEventPublisher {
    void publish(CancelResponseEvent cancelResponseEvent);
}
