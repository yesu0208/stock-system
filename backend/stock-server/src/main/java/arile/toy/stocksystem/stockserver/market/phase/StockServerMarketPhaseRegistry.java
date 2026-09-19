package arile.toy.stocksystem.stockserver.market.phase;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockServerMarketPhaseRegistry {

    private final ConcurrentHashMap<String, StockServerMarketPhase> phaseMap = new ConcurrentHashMap<>();

    public StockServerMarketPhase getPhase(String stockCode) {
        return phaseMap.get(stockCode);
    }

    public void setClosed(String stockCode) {
        phaseMap.put(stockCode, StockServerMarketPhase.CLOSED);
    }

    public void setOpen(String stockCode) {
        phaseMap.put(stockCode, StockServerMarketPhase.OPEN);
    }

    public boolean isOpened(String stockCode) {
        StockServerMarketPhase phase = phaseMap.get(stockCode);
        return phase != null && phase.isOrderable();
    }

    public boolean isClosed(String stockCode) {
        StockServerMarketPhase phase = phaseMap.get(stockCode);
        return phase == null || phase == StockServerMarketPhase.CLOSED;
    }

    public void setPhase(String stockCode, StockServerMarketPhase phase) {
        phaseMap.put(stockCode, phase);
    }
}
