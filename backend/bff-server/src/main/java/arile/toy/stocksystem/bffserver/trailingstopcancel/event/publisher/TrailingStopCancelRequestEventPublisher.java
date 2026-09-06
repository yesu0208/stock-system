package arile.toy.stocksystem.bffserver.trailingstopcancel.event.publisher;

import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;

public interface TrailingStopCancelRequestEventPublisher {
    void publishTrailingStopCancel(TrailingStopCancelRequestEvent event);
}
