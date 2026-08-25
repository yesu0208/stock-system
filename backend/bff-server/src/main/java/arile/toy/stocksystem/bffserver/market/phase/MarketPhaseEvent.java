package arile.toy.stocksystem.bffserver.market.phase;

public record MarketPhaseEvent(
        String stockCode,
        BffServerMarketPhase marketPhase
) {
}
