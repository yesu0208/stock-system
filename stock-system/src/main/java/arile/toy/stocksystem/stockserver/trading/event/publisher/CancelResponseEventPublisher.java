package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.event.CancelResponseEvent;

public interface CancelResponseEventPublisher {
    void publish(CancelResponseEvent cancelResponseEvent);
}
