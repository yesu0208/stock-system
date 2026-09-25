package arile.toy.stocksystem.stockserver.alert.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Registry] 알림 큐 테스트")
class AlertQueueRegistryTest {

    private final AlertQueueRegistry sut = new AlertQueueRegistry();

    @DisplayName("ABOVE는 발동가 낮은 순, BELOW는 발동가 높은 순, 같은 가격이면 먼저 등록한 순으로 꺼낸다")
    @Test
    void whenPolling_thenOrdersByTriggerPriceThenTime() {
        Instant t0 = Instant.parse("2026-09-25T00:00:00Z");
        sut.alertEnqueue(alert(1L, AlertDirection.ABOVE, 72_000, t0));
        sut.alertEnqueue(alert(2L, AlertDirection.ABOVE, 71_000, t0.plusSeconds(2)));
        sut.alertEnqueue(alert(3L, AlertDirection.ABOVE, 71_000, t0.plusSeconds(1)));
        sut.alertEnqueue(alert(4L, AlertDirection.BELOW, 68_000, t0));
        sut.alertEnqueue(alert(5L, AlertDirection.BELOW, 69_000, t0.plusSeconds(2)));
        sut.alertEnqueue(alert(6L, AlertDirection.BELOW, 69_000, t0.plusSeconds(1)));

        assertThat(sut.peekAbove("005930")).map(AlertDto::alertId).contains(3L);
        assertThat(sut.pollAbove("005930").alertId()).isEqualTo(3L);
        assertThat(sut.pollAbove("005930").alertId()).isEqualTo(2L);
        assertThat(sut.pollAbove("005930").alertId()).isEqualTo(1L);
        assertThat(sut.pollAbove("005930")).isNull();

        assertThat(sut.peekBelow("005930")).map(AlertDto::alertId).contains(6L);
        assertThat(sut.pollBelow("005930").alertId()).isEqualTo(6L);
        assertThat(sut.pollBelow("005930").alertId()).isEqualTo(5L);
        assertThat(sut.pollBelow("005930").alertId()).isEqualTo(4L);
        assertThat(sut.peekBelow("005930")).isEmpty();
    }

    @DisplayName("취소하면 해당 알림만 방향과 관계없이 제거하고, 종목별로 분리된다")
    @Test
    void whenCanceling_thenRemovesOnlyThatAlert() {
        Instant t0 = Instant.now();
        sut.alertEnqueue(alert(1L, AlertDirection.ABOVE, 72_000, t0));
        sut.alertEnqueue(alert(2L, AlertDirection.BELOW, 68_000, t0));

        sut.alertCancel(1L, "005930");
        sut.alertCancel(2L, "000660");

        assertThat(sut.peekAbove("005930")).isEmpty();
        assertThat(sut.peekBelow("005930")).map(AlertDto::alertId).contains(2L);
        assertThat(sut.peekBelow("000660")).isEmpty();
    }

    @DisplayName("단일 종목 큐: removeByAlertId는 제거 여부를 반환한다")
    @Test
    void whenRemovingFromQueue_thenReturnsWhetherRemoved() {
        InMemorySingleStockAlertQueue queue = new InMemorySingleStockAlertQueue();
        queue.alertEnqueue(alert(1L, AlertDirection.BELOW, 68_000, Instant.now()));

        assertThat(queue.removeByAlertId(1L)).isTrue();
        assertThat(queue.removeByAlertId(1L)).isFalse();
    }

    private AlertDto alert(Long id, AlertDirection direction, int triggerPrice, Instant time) {
        return new AlertDto(id, "user", "005930", direction, triggerPrice, time);
    }
}
