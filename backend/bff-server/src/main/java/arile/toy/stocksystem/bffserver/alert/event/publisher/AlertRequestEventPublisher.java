package arile.toy.stocksystem.bffserver.alert.event.publisher;

import arile.toy.stocksystem.bffserver.alert.event.AlertRequestEvent;

public interface AlertRequestEventPublisher {
    void publishAlert(AlertRequestEvent event);
}
