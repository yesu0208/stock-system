package arile.toy.stocksystem.bffserver.trailingstop.event.publisher;

import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopRequestEvent;

public interface TrailingStopRequestEventPublisher {
    void publishTrailingStop(TrailingStopRequestEvent event);
}
