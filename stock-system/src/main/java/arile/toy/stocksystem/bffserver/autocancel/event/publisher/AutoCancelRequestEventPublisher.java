package arile.toy.stocksystem.bffserver.autocancel.event.publisher;

import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelRequestEvent;

public interface AutoCancelRequestEventPublisher {
    void publishAutoCancel(AutoCancelRequestEvent event);
}
