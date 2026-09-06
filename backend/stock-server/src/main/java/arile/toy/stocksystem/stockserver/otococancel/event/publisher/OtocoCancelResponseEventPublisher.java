package arile.toy.stocksystem.stockserver.otococancel.event.publisher;

import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelResponseEvent;

public interface OtocoCancelResponseEventPublisher {
    void publish(OtocoCancelResponseEvent event);
}
