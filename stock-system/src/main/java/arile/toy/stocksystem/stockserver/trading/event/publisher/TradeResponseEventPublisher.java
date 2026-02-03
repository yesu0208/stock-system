package arile.toy.stocksystem.stockserver.trading.event.publisher;

import arile.toy.stocksystem.stockserver.trading.event.TradeResponseEvent;

public interface TradeResponseEventPublisher {
    void publish(TradeResponseEvent tradeResponseEvent);
}
