package arile.toy.stocksystem.stockserver.alert.service;

import arile.toy.stocksystem.stockserver.alert.dto.*;
import arile.toy.stocksystem.stockserver.alert.event.publisher.AlertResponseEventPublisher;
import arile.toy.stocksystem.stockserver.alert.repository.StockServerAlertResponseRepository;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.AlertLockRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 알림 발송 감시 테스트")
@ExtendWith(MockitoExtension.class)
class AlertTriggerServiceTest {

    @Mock private AlertLockRegistry alertLockRegistry;
    @Mock private AlertService alertService;
    @Mock private StockServerAlertResponseRepository stockServerAlertResponseRepository;
    @Mock private AlertResponseEventPublisher alertResponseEventPublisher;

    // 실제 큐를 사용해 꺼내기·되돌리기 동작까지 검증
    private final AlertQueueRegistry alertQueueRegistry = new AlertQueueRegistry();
    private AlertTriggerService sut;

    @BeforeEach
    void setUp() {
        sut = new AlertTriggerService(alertQueueRegistry, alertLockRegistry, alertService,
                stockServerAlertResponseRepository, alertResponseEventPublisher);
        given(alertLockRegistry.lock("005930")).willReturn(new ReentrantLock());
    }

    @DisplayName("ABOVE는 현재가 ≥ 발동가, BELOW는 현재가 ≤ 발동가인 알림만 발송하고 나머지는 큐에 남긴다")
    @Test
    void givenAlerts_whenTick_thenFiresReached() {
        enqueue(1L, AlertDirection.ABOVE, 70_000);
        enqueue(2L, AlertDirection.ABOVE, 70_100);
        enqueue(3L, AlertDirection.BELOW, 70_000);
        enqueue(4L, AlertDirection.BELOW, 69_900);
        givenFireResult(AlertStatus.ACTIVE);

        sut.getExternalTickMessageAndCheckAlerts(tick(70_000));

        then(alertService).should().updateAlertStatusByFire(1L);
        then(alertService).should().updateAlertStatusByFire(3L);
        then(alertService).shouldHaveNoMoreInteractions();
        then(stockServerAlertResponseRepository).should().delete("user", 1L);
        then(alertResponseEventPublisher).should().publishFired(argThat(d -> d.alertId().equals(1L)), eq(70_000));
        then(alertResponseEventPublisher).should().publishFired(argThat(d -> d.alertId().equals(3L)), eq(70_000));
        assertThat(alertQueueRegistry.peekAbove("005930")).map(AlertDto::alertId).contains(2L);
        assertThat(alertQueueRegistry.peekBelow("005930")).map(AlertDto::alertId).contains(4L);
    }

    @DisplayName("이미 취소된 알림은 발송하지 않고 다음 알림을 계속 처리한다")
    @Test
    void givenCanceled_whenTick_thenSkipsAndContinues() {
        enqueue(1L, AlertDirection.ABOVE, 69_000);
        enqueue(2L, AlertDirection.ABOVE, 69_500);
        given(alertService.updateAlertStatusByFire(1L)).willReturn(UpdateAlertStatusResult.of(null, AlertStatus.CANCELED));
        given(alertService.updateAlertStatusByFire(2L)).willReturn(UpdateAlertStatusResult.of(null, AlertStatus.ACTIVE));

        sut.getExternalTickMessageAndCheckAlerts(tick(70_000));

        then(alertResponseEventPublisher).should().publishFired(argThat(d -> d.alertId().equals(2L)), eq(70_000));
        then(alertResponseEventPublisher).shouldHaveNoMoreInteractions();
        assertThat(alertQueueRegistry.peekAbove("005930")).isEmpty();
    }

    @DisplayName("상태 변경이 실패하면 큐에 되돌리고 해당 방향 처리를 멈춘다 (다음 틱에 재시도)")
    @Test
    void givenStatusUpdateFails_whenTick_thenReEnqueuesAndStops() {
        enqueue(1L, AlertDirection.ABOVE, 69_000);
        enqueue(2L, AlertDirection.ABOVE, 69_500);
        enqueue(3L, AlertDirection.BELOW, 71_000);
        given(alertService.updateAlertStatusByFire(1L)).willThrow(new IllegalStateException("db error"));
        given(alertService.updateAlertStatusByFire(3L)).willReturn(UpdateAlertStatusResult.of(null, AlertStatus.ACTIVE));

        sut.getExternalTickMessageAndCheckAlerts(tick(70_000));

        then(alertService).should(never()).updateAlertStatusByFire(2L);
        assertThat(alertQueueRegistry.peekAbove("005930")).map(AlertDto::alertId).contains(1L);
        // BELOW 방향은 영향 없이 처리
        then(alertResponseEventPublisher).should().publishFired(argThat(d -> d.alertId().equals(3L)), eq(70_000));
    }

    @DisplayName("응답 캐시 삭제가 실패해도 발송 이벤트는 발행한다")
    @Test
    void givenDeleteFails_whenFiring_thenStillPublishes() {
        enqueue(1L, AlertDirection.BELOW, 70_000);
        givenFireResult(AlertStatus.ACTIVE);
        willThrow(new IllegalStateException("redis down")).given(stockServerAlertResponseRepository).delete("user", 1L);

        sut.getExternalTickMessageAndCheckAlerts(tick(70_000));

        then(alertResponseEventPublisher).should().publishFired(any(), eq(70_000));
    }

    private void enqueue(Long id, AlertDirection direction, int triggerPrice) {
        alertQueueRegistry.alertEnqueue(new AlertDto(id, "user", "005930", direction, triggerPrice, Instant.now()));
    }

    private void givenFireResult(AlertStatus previous) {
        given(alertService.updateAlertStatusByFire(anyLong())).willReturn(UpdateAlertStatusResult.of(null, previous));
    }

    private TradePriceTickMessage tick(int price) {
        TradePriceTickMessage tick = mock(TradePriceTickMessage.class);
        given(tick.stockCode()).willReturn("005930");
        given(tick.curPrice()).willReturn(price);
        return tick;
    }
}
