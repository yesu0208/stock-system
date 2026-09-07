package arile.toy.stocksystem.stockserver.alertcancel.event.publisher;

import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelResponseEvent;

public interface AlertCancelResponseEventPublisher {
    void publish(AlertCancelResponseEvent alertCancelResponseEvent);
}
