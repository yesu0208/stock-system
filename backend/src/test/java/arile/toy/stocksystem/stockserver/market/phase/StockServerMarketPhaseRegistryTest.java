package arile.toy.stocksystem.stockserver.market.phase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockServerMarketPhaseRegistryTest {

    private StockServerMarketPhaseRegistry registry;

    @BeforeEach
    void setup() {
        registry = new StockServerMarketPhaseRegistry();
    }

    @Test
    @DisplayName("주식 코드의 시장을 Open으로 설정하면 isOpened는 true, isClosed는 false")
    void whenSetOpen_thenIsOpenedTrueAndIsClosedFalse() {
        String stockCode = "000660";

        registry.setOpen(stockCode);

        assertTrue(registry.isOpened(stockCode));
        assertFalse(registry.isClosed(stockCode));
    }

    @Test
    @DisplayName("주식 코드의 시장을 Closed로 설정하면 isClosed는 true, isOpened는 false")
    void whenSetClosed_thenIsClosedTrueAndIsOpenedFalse() {
        String stockCode = "000660";

        registry.setClosed(stockCode);

        assertTrue(registry.isClosed(stockCode));
        assertFalse(registry.isOpened(stockCode));
    }

    @Test
    @DisplayName("setPhase 호출 시 주식 코드의 시장 단계가 올바르게 업데이트된다")
    void whenSetPhase_thenPhaseUpdatedCorrectly() {
        String stockCode = "000660";

        registry.setPhase(stockCode, StockServerMarketPhase.OPEN);
        assertTrue(registry.isOpened(stockCode));

        registry.setPhase(stockCode, StockServerMarketPhase.CLOSED);
        assertTrue(registry.isClosed(stockCode));
    }

    @Test
    @DisplayName("시장 단계가 설정되지 않은 경우 isOpened와 isClosed는 false 반환")
    void whenPhaseNotSet_thenIsOpenedAndIsClosedReturnFalse() {
        String stockCode = "000660";

        assertFalse(registry.isOpened(stockCode));
        assertFalse(registry.isClosed(stockCode));
    }
}
