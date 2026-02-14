package arile.toy.stocksystem.stockserver.market.phase;

public record MarketPhaseEvent(
        String stockCode,
        StockServerMarketPhase marketPhase
) {
    public static MarketPhaseEvent of(String stockCode, StockServerMarketPhase marketPhase) {
        return new MarketPhaseEvent(stockCode, marketPhase);
    }
}
