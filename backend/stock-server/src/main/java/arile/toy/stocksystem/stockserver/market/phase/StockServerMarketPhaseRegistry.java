package arile.toy.stocksystem.stockserver.market.phase;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class StockServerMarketPhaseRegistry {

    private final ConcurrentHashMap<String, StockServerMarketPhase> phaseMap = new ConcurrentHashMap<>();

    // 부하 테스트 전용: true 면 장 상태와 관계없이 주문을 받는다 (운영에서는 켜지 않음, 기본 false)
    @Value("${market-phase.force-open:false}")
    private boolean forceOpen;

    @PostConstruct
    void warnIfForceOpen() {
        if (forceOpen) {
            log.warn("[LOADTEST] market-phase.force-open=true: 장 상태와 관계없이 주문을 처리합니다. 운영 환경이면 즉시 끄세요.");
        }
    }

    public StockServerMarketPhase getPhase(String stockCode) {
        return phaseMap.get(stockCode);
    }

    public boolean isClosed(String stockCode) {
        if (forceOpen) {
            return false;
        }
        StockServerMarketPhase phase = phaseMap.get(stockCode);
        return phase == null || phase == StockServerMarketPhase.CLOSED;
    }

    public void setPhase(String stockCode, StockServerMarketPhase phase) {
        phaseMap.put(stockCode, phase);
    }
}
