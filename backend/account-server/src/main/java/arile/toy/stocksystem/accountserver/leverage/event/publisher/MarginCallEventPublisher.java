package arile.toy.stocksystem.accountserver.leverage.event.publisher;

import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;

public interface MarginCallEventPublisher {
    void publish(MarginCallEvent event);
}
