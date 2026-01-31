package arile.toy.stocksystem.stockserver.trading.event.publisher;

public interface OrderResponseEventPublisher {
    void publish(String username);
}
