package arile.toy.stocksystem.stockserver.market.phase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Registry] 종목별 장 상태 레지스트리 테스트")
class StockServerMarketPhaseRegistryTest {

    private final StockServerMarketPhaseRegistry sut = new StockServerMarketPhaseRegistry();

    @DisplayName("상태가 등록되지 않은 종목은 마감으로 간주한다 (주문 차단 기본값)")
    @Test
    void givenUnknownStock_whenCheckingClosed_thenTrue() {
        assertThat(sut.getPhase("005930")).isNull();
        assertThat(sut.isClosed("005930")).isTrue();
    }

    @DisplayName("CLOSED 상태면 마감이다")
    @Test
    void givenClosed_whenCheckingClosed_thenTrue() {
        sut.setPhase("005930", StockServerMarketPhase.CLOSED);

        assertThat(sut.isClosed("005930")).isTrue();
    }

    @DisplayName("동시호가·정규장·애프터 상태는 마감이 아니다")
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = StockServerMarketPhase.class, names = "CLOSED", mode = EnumSource.Mode.EXCLUDE)
    void givenOrderablePhase_whenCheckingClosed_thenFalse(StockServerMarketPhase phase) {
        sut.setPhase("005930", phase);

        assertThat(sut.isClosed("005930")).isFalse();
        assertThat(sut.getPhase("005930")).isEqualTo(phase);
    }

    @DisplayName("종목별로 상태를 따로 관리한다")
    @Test
    void givenDifferentStocks_whenSettingPhase_thenIndependent() {
        sut.setPhase("005930", StockServerMarketPhase.OPEN);
        sut.setPhase("000660", StockServerMarketPhase.CLOSED);

        assertThat(sut.isClosed("005930")).isFalse();
        assertThat(sut.isClosed("000660")).isTrue();
    }
}
