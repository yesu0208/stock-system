package arile.toy.stocksystem.stockserver.trading.event;

public record OrderResponseEvent(
        String username,
        boolean success,
        String errorMessage
) {
}
