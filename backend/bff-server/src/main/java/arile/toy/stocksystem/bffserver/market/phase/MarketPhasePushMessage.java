package arile.toy.stocksystem.bffserver.market.phase;

public record MarketPhasePushMessage(String phase, String label) {
    public static MarketPhasePushMessage of(BffServerMarketPhase phase) {
        String label = switch (phase) {
            case MORNING_CALL -> "동시호가";
            case OPEN         -> "OPEN";
            case CLOSING_CALL -> "동시호가";
            case AFTER        -> "AFTER";
            case CLOSED       -> "CLOSED";
        };
        return new MarketPhasePushMessage(phase.name(), label);
    }
}
