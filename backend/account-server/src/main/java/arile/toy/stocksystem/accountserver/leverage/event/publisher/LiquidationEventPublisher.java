package arile.toy.stocksystem.accountserver.leverage.event.publisher;

import arile.toy.stocksystem.accountserver.leverage.event.LiquidationExecutedEvent;

public interface LiquidationEventPublisher {
    void publish(LiquidationExecutedEvent event);
}
