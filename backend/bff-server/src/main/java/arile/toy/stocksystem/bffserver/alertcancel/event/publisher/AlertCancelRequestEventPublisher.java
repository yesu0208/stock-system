package arile.toy.stocksystem.bffserver.alertcancel.event.publisher;

import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelRequestEvent;

public interface AlertCancelRequestEventPublisher {
    void publishAlertCancel(AlertCancelRequestEvent event);
}
