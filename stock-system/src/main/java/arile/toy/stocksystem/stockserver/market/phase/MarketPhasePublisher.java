package arile.toy.stocksystem.stockserver.market.phase;

public interface MarketPhasePublisher {
    void publish(String stockCode, StockServerMarketPhase phase);
}
