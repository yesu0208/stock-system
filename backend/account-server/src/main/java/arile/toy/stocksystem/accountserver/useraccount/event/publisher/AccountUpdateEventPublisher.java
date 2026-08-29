package arile.toy.stocksystem.accountserver.useraccount.event.publisher;

public interface AccountUpdateEventPublisher {
    void publish(String username);
}
