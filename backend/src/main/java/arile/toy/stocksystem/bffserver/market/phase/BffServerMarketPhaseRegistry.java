package arile.toy.stocksystem.bffserver.market.phase;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class BffServerMarketPhaseRegistry {

    private final ConcurrentHashMap<String, BffServerMarketPhase> phaseMap = new ConcurrentHashMap<>();

    public void setClosed(String stockCode) {
        phaseMap.put(stockCode, BffServerMarketPhase.CLOSED);
    }

    public void setOpen(String stockCode) {
        phaseMap.put(stockCode, BffServerMarketPhase.OPEN);
    }

    public boolean isClosed(String stockCode) {
        BffServerMarketPhase phase = phaseMap.get(stockCode);
        return phase == BffServerMarketPhase.CLOSED;
    }

    public boolean isOpen(String stockCode) {
        BffServerMarketPhase phase = phaseMap.get(stockCode);
        return phase == BffServerMarketPhase.OPEN;
    }

    public void setPhase(String stockCode, BffServerMarketPhase phase) {
        phaseMap.put(stockCode, phase);
    }
}
