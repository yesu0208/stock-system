package arile.toy.stocksystem.bffserver.market.phase;

public record MarketPhasePushMessage(String phase, String label) {
    public static MarketPhasePushMessage of(BffServerMarketPhase phase) {
        String label = switch (phase) {
            case MORNING_CALL -> "동시호가";
            case OPEN         -> "정규장";
            case CLOSING_CALL -> "동시호가";
            case AFTER        -> "AFTER";
            case CLOSED       -> "장마감";
        };
        return new MarketPhasePushMessage(phase.name(), label);
    }
}
