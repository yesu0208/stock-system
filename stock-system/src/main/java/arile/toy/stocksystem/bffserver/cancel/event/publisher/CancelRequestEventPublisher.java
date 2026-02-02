package arile.toy.stocksystem.bffserver.cancel.event.publisher;

import arile.toy.stocksystem.bffserver.cancel.event.CancelRequestEvent;

public interface CancelRequestEventPublisher {
    void publishCancel(CancelRequestEvent event);
}
