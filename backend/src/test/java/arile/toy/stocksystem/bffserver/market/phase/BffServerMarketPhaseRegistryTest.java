package arile.toy.stocksystem.bffserver.market.phase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BffServerMarketPhaseRegistryTest {

    private BffServerMarketPhaseRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BffServerMarketPhaseRegistry();
    }

    @Test
    @DisplayName("종목 상태를 Open으로 설정 후 확인")
    void givenStockCode_whenSetOpen_thenIsOpen() {
        // given
        String stockCode = "005930";

        // when
        registry.setOpen(stockCode);

        // then
        assertTrue(registry.isOpen(stockCode));
        assertFalse(registry.isClosed(stockCode));
    }

    @Test
    @DisplayName("종목 상태를 Closed로 설정 후 확인")
    void givenStockCode_whenSetClosed_thenIsClosed() {
        // given
        String stockCode = "005930";

        // when
        registry.setClosed(stockCode);

        // then
        assertTrue(registry.isClosed(stockCode));
        assertFalse(registry.isOpen(stockCode));
    }

    @Test
    @DisplayName("setPhase 메서드로 상태를 직접 변경")
    void givenStockCode_whenSetPhase_thenPhaseUpdated() {
        // given
        String stockCode = "005930";

        // when
        registry.setPhase(stockCode, BffServerMarketPhase.OPEN);

        // then
        assertTrue(registry.isOpen(stockCode));

        // when
        registry.setPhase(stockCode, BffServerMarketPhase.CLOSED);

        // then
        assertTrue(registry.isClosed(stockCode));
    }
}
