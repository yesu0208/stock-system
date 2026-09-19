package arile.toy.stocksystem.bffserver.market.phase;

public enum BffServerMarketPhase {
    MORNING_CALL,
    OPEN,
    CLOSING_CALL,
    AFTER,
    CLOSED;

    public boolean isOrderable() {
        return this != CLOSED;
    }
}
