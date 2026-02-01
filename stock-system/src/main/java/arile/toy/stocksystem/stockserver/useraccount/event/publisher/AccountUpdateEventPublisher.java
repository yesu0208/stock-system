package arile.toy.stocksystem.stockserver.useraccount.event.publisher;

public interface AccountUpdateEventPublisher {
    void publish(String username);
}
