package arile.toy.stocksystem.stockserver.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Lock] 종목별 락 레지스트리 테스트")
class LockRegistryTest {

    static Stream<Arguments> registries() {
        return Stream.of(
                Arguments.of("StockLockRegistry", (Function<String, ReentrantLock>) new StockLockRegistry()::lock),
                Arguments.of("AutoStockLockRegistry", (Function<String, ReentrantLock>) new AutoStockLockRegistry()::lock),
                Arguments.of("TrailingStopLockRegistry", (Function<String, ReentrantLock>) new TrailingStopLockRegistry()::lock),
                Arguments.of("OtocoLockRegistry", (Function<String, ReentrantLock>) new OtocoLockRegistry()::lock),
                Arguments.of("AlertLockRegistry", (Function<String, ReentrantLock>) new AlertLockRegistry()::lock)
        );
    }

    @DisplayName("같은 종목은 같은 공정 락을, 다른 종목은 다른 락을 돌려준다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("registries")
    void whenGettingLock_thenOnePerStock(String name, Function<String, ReentrantLock> lockFor) {
        ReentrantLock first = lockFor.apply("005930");

        assertThat(lockFor.apply("005930")).isSameAs(first);
        assertThat(lockFor.apply("000660")).isNotSameAs(first);
        assertThat(first.isFair()).isTrue();
    }
}
