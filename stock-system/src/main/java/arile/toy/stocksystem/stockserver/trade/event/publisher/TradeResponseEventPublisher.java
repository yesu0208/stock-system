package arile.toy.stocksystem.stockserver.trade.event.publisher;

import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;

public interface TradeResponseEventPublisher {
    void publish(TradeResponseEvent tradeResponseEvent);
}
