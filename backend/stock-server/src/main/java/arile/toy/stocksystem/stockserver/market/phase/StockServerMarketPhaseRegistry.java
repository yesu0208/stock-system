package arile.toy.stocksystem.stockserver.market.phase;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockServerMarketPhaseRegistry {

    private final ConcurrentHashMap<String, StockServerMarketPhase> phaseMap = new ConcurrentHashMap<>();

    public StockServerMarketPhase getPhase(String stockCode) {
        return phaseMap.get(stockCode);
    }

    public boolean isClosed(String stockCode) {
        StockServerMarketPhase phase = phaseMap.get(stockCode);
        return phase == null || phase == StockServerMarketPhase.CLOSED;
    }

    public void setPhase(String stockCode, StockServerMarketPhase phase) {
        phaseMap.put(stockCode, phase);
    }
}
