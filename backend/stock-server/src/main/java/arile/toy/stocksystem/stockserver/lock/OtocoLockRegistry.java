package arile.toy.stocksystem.stockserver.lock;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class OtocoLockRegistry {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock lock(String stockCode) {
        return locks.computeIfAbsent(stockCode, key -> new ReentrantLock(true));
    }
}
