package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.event.AutoCancelResponseEvent;

public interface AutoCancelResponseEventPublisher {
    void publish(AutoCancelResponseEvent autoCancelResponseEvent);
}
