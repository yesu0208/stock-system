package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Registry] 자동주문 대기열 레지스트리 테스트")
class AutoOrderQueueRegistryTest {

    private static final Instant T0 = Instant.parse("2026-09-25T00:00:00Z");

    private final AutoOrderQueueRegistry sut = new AutoOrderQueueRegistry();

    @DisplayName("종목별로 대기열을 분리해 관리한다")
    @Test
    void givenDifferentStocks_whenEnqueuing_thenSeparated() {
        sut.autoOrderEnqueue(dto(1L, "005930", AutoOrderType.BUY));
        sut.autoOrderEnqueue(dto(2L, "000660", AutoOrderType.BUY));

        assertThat(sut.pollBuy("005930").autoOrderId()).isEqualTo(1L);
        assertThat(sut.pollBuy("005930")).isNull();
        assertThat(sut.pollBuy("000660").autoOrderId()).isEqualTo(2L);
    }

    @DisplayName("취소는 해당 종목 대기열에서만 제거한다")
    @Test
    void givenCancel_whenCancelling_thenRemovesOnlyInThatStock() {
        sut.autoOrderEnqueue(dto(1L, "005930", AutoOrderType.SELL));
        sut.autoOrderEnqueue(dto(1L, "000660", AutoOrderType.SELL));

        sut.autoOrderCancel(1L, "005930");

        assertThat(sut.peekSell("005930")).isEmpty();
        assertThat(sut.peekSell("000660")).isPresent();
    }

    @DisplayName("등록한 적 없는 종목은 빈 대기열로 취급한다")
    @Test
    void givenUnknownStock_whenPolling_thenEmpty() {
        assertThat(sut.pollBuy("999999")).isNull();
        assertThat(sut.pollSell("999999")).isNull();
        assertThat(sut.peekBuy("999999")).isEmpty();
        assertThat(sut.peekSell("999999")).isEmpty();
    }

    private AutoOrderDto dto(Long id, String stockCode, AutoOrderType type) {
        return new AutoOrderDto(id, "user", stockCode, type, LeverageRatio.SPOT,
                70_000, 70_000, 10, T0);
    }
}
