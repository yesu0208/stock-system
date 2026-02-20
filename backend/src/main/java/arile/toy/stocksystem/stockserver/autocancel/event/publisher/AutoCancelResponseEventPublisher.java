package arile.toy.stocksystem.stockserver.autocancel.event.publisher;

import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;

public interface AutoCancelResponseEventPublisher {
    void publish(AutoCancelResponseEvent autoCancelResponseEvent);
}
