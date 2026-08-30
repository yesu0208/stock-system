package arile.toy.stocksystem.bffserver.market.phase;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class BffServerMarketPhaseRegistry {

    private static final String SNAPSHOT_KEY = "market:phase:snapshot";

    private final StringRedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, BffServerMarketPhase> phaseMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            Map<Object, Object> snapshot = redisTemplate.opsForHash().entries(SNAPSHOT_KEY);

            snapshot.forEach((stockCode, phase) -> {
                try {
                    phaseMap.put((String) stockCode, BffServerMarketPhase.valueOf((String) phase));
                } catch (IllegalArgumentException e) {
                    log.warn("알 수 없는 market phase 값. stockCode={}, phase={}", stockCode, phase);
                }
            });

            log.info("Market phase snapshot loaded from Redis. size={}", phaseMap.size());
        } catch (Exception e) {
            log.warn("Market phase snapshot 로딩 실패. pub/sub 갱신에만 의존합니다.", e);
        }
    }

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
