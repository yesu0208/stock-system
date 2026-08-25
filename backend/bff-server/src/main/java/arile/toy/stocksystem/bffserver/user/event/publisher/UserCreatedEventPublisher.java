package arile.toy.stocksystem.bffserver.user.event.publisher;

import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;

public interface UserCreatedEventPublisher {
    void publishUserCreatedEvent(UserCreatedEvent event);
}
