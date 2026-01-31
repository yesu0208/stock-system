package arile.toy.stocksystem.bffserver.order.event;

public record OrderResponseEvent(
        String username,
        boolean success,
        String errorMessage
) {
}
