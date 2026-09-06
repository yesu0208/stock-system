package arile.toy.stocksystem.bffserver.otoco.event.publisher;

import arile.toy.stocksystem.bffserver.otoco.event.OtocoRequestEvent;

public interface OtocoRequestEventPublisher {
    void publishOtoco(OtocoRequestEvent event);
}
