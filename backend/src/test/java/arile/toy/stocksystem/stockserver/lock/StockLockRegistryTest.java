package arile.toy.stocksystem.stockserver.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

class StockLockRegistryTest {

    private final StockLockRegistry lockRegistry = new StockLockRegistry();

    @Test
    @DisplayName("주식 코드로 Lock 요청 시 ReentrantLock을 반환한다")
    void givenStockCode_whenLock_thenReturnsReentrantLock() {
        ReentrantLock lock = lockRegistry.lock("000660");

        assertNotNull(lock);
        assertInstanceOf(ReentrantLock.class, lock);
    }

    @Test
    @DisplayName("같은 주식 코드로 여러 번 Lock 요청 시 동일한 Lock 인스턴스를 반환한다")
    void givenSameStockCode_whenLockMultipleTimes_thenSameLockInstance() {
        ReentrantLock lock1 = lockRegistry.lock("000660");
        ReentrantLock lock2 = lockRegistry.lock("000660");

        assertSame(lock1, lock2);
    }

    @Test
    @DisplayName("다른 주식 코드로 Lock 요청 시 서로 다른 Lock 인스턴스를 반환한다")
    void givenDifferentStockCode_whenLock_thenDifferentLockInstances() {
        ReentrantLock lock1 = lockRegistry.lock("000660");
        ReentrantLock lock2 = lockRegistry.lock("005930");

        assertNotSame(lock1, lock2);
    }
}
