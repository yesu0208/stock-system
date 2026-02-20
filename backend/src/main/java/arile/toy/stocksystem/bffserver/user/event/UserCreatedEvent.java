package arile.toy.stocksystem.bffserver.user.event;

public record UserCreatedEvent(
        String username
) {
    public static UserCreatedEvent of(String username) {
        return new UserCreatedEvent(username);
    }
}
