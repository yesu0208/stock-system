package arile.toy.stocksystem.stockserver.alertcancel.service;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.AlertQueueRegistry;
import arile.toy.stocksystem.stockserver.alert.dto.AlertStatus;
import arile.toy.stocksystem.stockserver.alert.dto.UpdateAlertStatusResult;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alert.repository.StockServerAlertResponseRepository;
import arile.toy.stocksystem.stockserver.alert.service.AlertService;
import arile.toy.stocksystem.stockserver.alertcancel.dto.AlertCancelErrorCode;
import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelRequestEvent;
import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelResponseEvent;
import arile.toy.stocksystem.stockserver.alertcancel.event.publisher.AlertCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.alertcancel.repository.AlertCancelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 알림 취소 테스트")
@ExtendWith(MockitoExtension.class)
class AlertCancelServiceTest {

    @InjectMocks private AlertCancelService sut;

    @Mock private AlertService alertService;
    @Mock private AlertCancelRepository alertCancelRepository;
    @Mock private AlertQueueRegistry alertQueueRegistry;
    @Mock private AlertCancelResponseEventPublisher alertCancelResponseEventPublisher;
    @Mock private StockServerAlertResponseRepository stockServerAlertResponseRepository;

    private final AlertCancelRequestEvent request = AlertCancelRequestEvent.of(1L, "005930", "user");

    @DisplayName("소유자·종목이 맞지 않으면 아무것도 하지 않는다")
    @Test
    void givenNotOwner_whenCanceling_thenNothing() {
        given(alertService.updateAlertStatusByUserCancel(1L, "user", "005930")).willReturn(Optional.empty());

        sut.registerAlertCancel(request);

        then(alertCancelRepository).shouldHaveNoInteractions();
        then(alertQueueRegistry).shouldHaveNoInteractions();
        then(alertCancelResponseEventPublisher).shouldHaveNoInteractions();
    }

    @DisplayName("이미 취소·발송된 경우 각각의 에러 응답만 발행한다")
    @Test
    void givenClosed_whenCanceling_thenPublishesError() {
        AlertEntity entity = entity();
        given(alertService.updateAlertStatusByUserCancel(1L, "user", "005930"))
                .willReturn(Optional.of(UpdateAlertStatusResult.of(entity, AlertStatus.CANCELED)))
                .willReturn(Optional.of(UpdateAlertStatusResult.of(entity, AlertStatus.FIRED)));

        sut.registerAlertCancel(request);
        sut.registerAlertCancel(request);

        ArgumentCaptor<AlertCancelResponseEvent> captor = ArgumentCaptor.forClass(AlertCancelResponseEvent.class);
        then(alertCancelResponseEventPublisher).should(times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(AlertCancelResponseEvent::errorCode)
                .containsExactly(AlertCancelErrorCode.ALREADY_CANCELLED, AlertCancelErrorCode.ALREADY_FIRED);
        then(alertCancelRepository).shouldHaveNoInteractions();
    }

    @DisplayName("ACTIVE면 취소 이력 저장 → 큐 제거 → 응답 삭제 → 성공 발행 순으로 처리한다")
    @Test
    void givenActive_whenCanceling_thenSavesThenRemoves() {
        givenActive();

        sut.registerAlertCancel(request);

        InOrder inOrder = inOrder(alertCancelRepository, alertQueueRegistry,
                stockServerAlertResponseRepository, alertCancelResponseEventPublisher);
        inOrder.verify(alertCancelRepository).saveAndFlush(argThat(c -> c.getAlertId().equals(1L)));
        inOrder.verify(alertQueueRegistry).alertCancel(1L, "005930");
        inOrder.verify(stockServerAlertResponseRepository).delete("user", 1L);
        inOrder.verify(alertCancelResponseEventPublisher).publish(argThat(e -> e.success() && e.errorCode() == null));
    }

    @DisplayName("이력 저장이 실패하면 큐에서 빼지 않고 에러 발행 후 예외를 다시 던진다 (롤백 후 재시도)")
    @Test
    void givenSaveFails_whenCanceling_thenRethrowsWithoutRemoving() {
        givenActive();
        given(alertCancelRepository.saveAndFlush(any())).willThrow(new IllegalStateException("db error"));

        assertThatThrownBy(() -> sut.registerAlertCancel(request)).hasMessage("db error");

        then(alertQueueRegistry).shouldHaveNoInteractions();
        then(alertCancelResponseEventPublisher).should().publish(argThat(e ->
                !e.success() && e.errorCode() == AlertCancelErrorCode.INTERNAL_ERROR));
    }

    @DisplayName("이력 저장 이후 큐 제거·응답 삭제가 실패해도 예외 없이 성공을 발행한다")
    @Test
    void givenPostSaveFails_whenCanceling_thenStillSucceeds() {
        givenActive();
        willThrow(new IllegalStateException("queue error")).given(alertQueueRegistry).alertCancel(1L, "005930");
        willThrow(new IllegalStateException("redis down")).given(stockServerAlertResponseRepository).delete("user", 1L);

        sut.registerAlertCancel(request);

        then(alertCancelResponseEventPublisher).should().publish(argThat(AlertCancelResponseEvent::success));
    }

    private void givenActive() {
        given(alertService.updateAlertStatusByUserCancel(1L, "user", "005930"))
                .willReturn(Optional.of(UpdateAlertStatusResult.of(entity(), AlertStatus.ACTIVE)));
    }

    private AlertEntity entity() {
        AlertEntity entity = AlertEntity.of("user", "005930", AlertDirection.ABOVE, 72_000, AlertStatus.CANCELED);
        entity.setAlertId(1L);
        return entity;
    }
}
