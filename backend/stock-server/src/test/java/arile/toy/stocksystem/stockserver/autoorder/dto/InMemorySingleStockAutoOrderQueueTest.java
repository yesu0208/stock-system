package arile.toy.stocksystem.stockserver.autoorder.dto;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Queue] 종목별 자동주문 대기열 테스트")
class InMemorySingleStockAutoOrderQueueTest {

    private static final Instant T0 = Instant.parse("2026-09-25T00:00:00Z");

    private final InMemorySingleStockAutoOrderQueue sut = new InMemorySingleStockAutoOrderQueue();

    @DisplayName("매수는 주문가와 무관하게 발동가 낮은 순으로 꺼낸다 (가격 상승 시 먼저 발동되는 순)")
    @Test
    void givenBuyOrders_whenPolling_thenLowestTriggerFirst() {
        // 주문가 순이었다면 A(주문가 71,000)가 먼저 나와 B의 발동이 막혔던 케이스
        sut.autoOrderEnqueue(dto(1L, AutoOrderType.BUY, 72_000, 71_000, T0));
        sut.autoOrderEnqueue(dto(2L, AutoOrderType.BUY, 70_000, 72_000, T0));
        sut.autoOrderEnqueue(dto(3L, AutoOrderType.BUY, 71_000, 70_000, T0));

        assertThat(sut.pollBuy().autoOrderId()).isEqualTo(2L);
        assertThat(sut.pollBuy().autoOrderId()).isEqualTo(3L);
        assertThat(sut.pollBuy().autoOrderId()).isEqualTo(1L);
        assertThat(sut.pollBuy()).isNull();
    }

    @DisplayName("매도는 주문가와 무관하게 발동가 높은 순으로 꺼낸다 (가격 하락 시 먼저 발동되는 순)")
    @Test
    void givenSellOrders_whenPolling_thenHighestTriggerFirst() {
        sut.autoOrderEnqueue(dto(1L, AutoOrderType.SELL, 68_000, 69_000, T0));
        sut.autoOrderEnqueue(dto(2L, AutoOrderType.SELL, 70_000, 67_000, T0));
        sut.autoOrderEnqueue(dto(3L, AutoOrderType.SELL, 69_000, 68_000, T0));

        assertThat(sut.pollSell().autoOrderId()).isEqualTo(2L);
        assertThat(sut.pollSell().autoOrderId()).isEqualTo(3L);
        assertThat(sut.pollSell().autoOrderId()).isEqualTo(1L);
        assertThat(sut.pollSell()).isNull();
    }

    @DisplayName("발동가가 같으면 먼저 등록한 자동주문부터 꺼낸다")
    @Test
    void givenSameTrigger_whenPolling_thenEarlierFirst() {
        sut.autoOrderEnqueue(dto(2L, AutoOrderType.BUY, 70_000, 70_000, T0.plusSeconds(1)));
        sut.autoOrderEnqueue(dto(1L, AutoOrderType.BUY, 70_000, 70_000, T0));
        sut.autoOrderEnqueue(dto(4L, AutoOrderType.SELL, 70_000, 70_000, T0.plusSeconds(1)));
        sut.autoOrderEnqueue(dto(3L, AutoOrderType.SELL, 70_000, 70_000, T0));

        assertThat(sut.pollBuy().autoOrderId()).isEqualTo(1L);
        assertThat(sut.pollSell().autoOrderId()).isEqualTo(3L);
    }

    @DisplayName("매수·매도는 서로 다른 대기열에 들어간다")
    @Test
    void givenBuyAndSell_whenEnqueuing_thenSeparated() {
        sut.autoOrderEnqueue(dto(1L, AutoOrderType.BUY, 70_000, 70_000, T0));
        sut.autoOrderEnqueue(dto(2L, AutoOrderType.SELL, 70_000, 70_000, T0));

        assertThat(sut.peekBuy()).get().extracting(AutoOrderDto::autoOrderId).isEqualTo(1L);
        assertThat(sut.peekSell()).get().extracting(AutoOrderDto::autoOrderId).isEqualTo(2L);
    }

    @DisplayName("peek은 꺼내지 않고 맨 앞을 보여 주며, 비어 있으면 empty다")
    @Test
    void givenQueue_whenPeeking_thenDoesNotRemove() {
        assertThat(sut.peekBuy()).isEmpty();
        assertThat(sut.peekSell()).isEmpty();

        sut.autoOrderEnqueue(dto(1L, AutoOrderType.BUY, 70_000, 70_000, T0));

        assertThat(sut.peekBuy()).isPresent();
        assertThat(sut.peekBuy()).isPresent();
        assertThat(sut.pollBuy().autoOrderId()).isEqualTo(1L);
    }

    @DisplayName("자동주문 ID로 매수·매도 대기열에서 제거하고, 없으면 false를 반환한다")
    @Test
    void givenAutoOrderId_whenRemoving_thenRemovesFromEitherSide() {
        sut.autoOrderEnqueue(dto(1L, AutoOrderType.BUY, 70_000, 70_000, T0));
        sut.autoOrderEnqueue(dto(2L, AutoOrderType.SELL, 70_000, 70_000, T0));

        assertThat(sut.removeByAutoOrderId(1L)).isTrue();
        assertThat(sut.removeByAutoOrderId(2L)).isTrue();
        assertThat(sut.removeByAutoOrderId(99L)).isFalse();
        assertThat(sut.peekBuy()).isEmpty();
        assertThat(sut.peekSell()).isEmpty();
    }

    private AutoOrderDto dto(Long id, AutoOrderType type, int triggerPrice, int orderPrice, Instant time) {
        return new AutoOrderDto(id, "user", "005930", type, LeverageRatio.SPOT,
                triggerPrice, orderPrice, 10, time);
    }
}
