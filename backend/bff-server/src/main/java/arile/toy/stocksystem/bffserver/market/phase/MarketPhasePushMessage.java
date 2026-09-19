package arile.toy.stocksystem.bffserver.market.phase;

public record MarketPhasePushMessage(String phase, String label) {
    public static MarketPhasePushMessage of(BffServerMarketPhase phase) {
        String label = switch (phase) {
            case MORNING_CALL -> "개장 동시호가";
            case OPEN         -> "정규장";
            case CLOSING_CALL -> "마감 동시호가";
            case AFTER        -> "애프터마켓";
            case CLOSED       -> "장마감";
        };
        return new MarketPhasePushMessage(phase.name(), label);
    }
}
