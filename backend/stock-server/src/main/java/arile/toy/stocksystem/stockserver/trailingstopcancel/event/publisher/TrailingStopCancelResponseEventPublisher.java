package arile.toy.stocksystem.stockserver.trailingstopcancel.event.publisher;

import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;

public interface TrailingStopCancelResponseEventPublisher {
    void publish(TrailingStopCancelResponseEvent event);
}
