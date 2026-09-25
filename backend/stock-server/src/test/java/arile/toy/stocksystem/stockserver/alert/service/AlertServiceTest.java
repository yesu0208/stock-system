package arile.toy.stocksystem.stockserver.alert.service;

import arile.toy.stocksystem.stockserver.alert.dto.*;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.event.publisher.AlertResponseEventPublisher;
import arile.toy.stocksystem.stockserver.alert.repository.AlertRepository;
import arile.toy.stocksystem.stockserver.alert.repository.StockServerAlertResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 알림 등록·상태 변경 테스트")
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @InjectMocks private AlertService sut;

    @Mock private AlertRepository alertRepository;
    @Mock private AlertQueueRegistry alertQueueRegistry;
    @Mock private AlertResponseEventPublisher alertResponseEventPublisher;
    @Mock private StockServerAlertResponseRepository stockServerAlertResponseRepository;

    private final AlertRequestEvent request = AlertRequestEvent.of("user", "005930", AlertDirection.ABOVE, 72_000);

    @Nested
    @DisplayName("registerAlert")
    class Register {

        @DisplayName("ACTIVE로 저장하고 큐 등록·응답 저장·등록 이벤트를 발행한다")
        @Test
        void whenRegistering_thenSavesEnqueuesAndPublishes() {
            givenSaveAssignsId();

            sut.registerAlert(request);

            then(alertRepository).should().save(argThat(e -> e.getStatus() == AlertStatus.ACTIVE && e.getTriggerPrice() == 72_000));
            then(alertQueueRegistry).should().alertEnqueue(argThat(d -> d.alertId().equals(1L)));
            then(stockServerAlertResponseRepository).should().save(argThat(m -> m.alertId().equals(1L)));
            then(alertResponseEventPublisher).should().publishRegistered(any());
        }

        @DisplayName("저장이 실패하면 에러를 발행하고 다시 던진다")
        @Test
        void givenSaveFails_whenRegistering_thenPublishesErrorAndRethrows() {
            given(alertRepository.save(any())).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerAlert(request)).hasMessage("db error");

            then(alertQueueRegistry).shouldHaveNoInteractions();
            then(alertResponseEventPublisher).should().publishRegisterError(request, AlertErrorCode.INTERNAL_ERROR);
        }

        @DisplayName("저장 후 큐 등록이 실패하면 큐에서 제거·CANCELED로 무효화하고 에러 발행 후 다시 던진다")
        @Test
        void givenEnqueueFails_whenRegistering_thenCancels() {
            givenSaveAssignsId();
            willThrow(new IllegalStateException("queue error")).given(alertQueueRegistry).alertEnqueue(any());

            assertThatThrownBy(() -> sut.registerAlert(request)).hasMessage("queue error");

            ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
            then(alertRepository).should(times(2)).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AlertStatus.CANCELED);
            then(alertQueueRegistry).should().alertCancel(1L, "005930");
            then(alertResponseEventPublisher).should().publishRegisterError(request, AlertErrorCode.INTERNAL_ERROR);
            then(stockServerAlertResponseRepository).shouldHaveNoInteractions();
        }

        @DisplayName("무효화 저장까지 실패하면 원래 예외에 첨부한다")
        @Test
        void givenCancelSaveFails_whenRegistering_thenSuppressed() {
            given(alertRepository.save(any(AlertEntity.class)))
                    .willAnswer(invocation -> {
                        AlertEntity entity = invocation.getArgument(0);
                        entity.setAlertId(1L);
                        return entity;
                    })
                    .willThrow(new IllegalStateException("cancel save error"));
            willThrow(new IllegalStateException("queue error")).given(alertQueueRegistry).alertEnqueue(any());

            assertThatThrownBy(() -> sut.registerAlert(request))
                    .hasMessage("queue error")
                    .satisfies(e -> assertThat(e.getSuppressed()).singleElement()
                            .extracting(Throwable::getMessage).isEqualTo("cancel save error"));
        }

        @DisplayName("등록 후 응답 캐시 저장이 실패해도 예외 없이 등록 이벤트를 발행한다 (재시도로 인한 중복 등록 방지)")
        @Test
        void givenResponseSaveFails_whenRegistering_thenDoesNotThrow() {
            givenSaveAssignsId();
            willThrow(new IllegalStateException("redis down")).given(stockServerAlertResponseRepository).save(any());

            sut.registerAlert(request);

            then(alertResponseEventPublisher).should().publishRegistered(any());
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class StatusChange {

        @DisplayName("발송: ACTIVE만 FIRED로 바꾼다")
        @Test
        void givenActive_whenFiring_thenFired() {
            AlertEntity entity = givenEntity(AlertStatus.ACTIVE);

            assertThat(sut.updateAlertStatusByFire(1L).previousStatus()).isEqualTo(AlertStatus.ACTIVE);
            assertThat(entity.getStatus()).isEqualTo(AlertStatus.FIRED);
        }

        @DisplayName("발송·취소: 이미 종료된 알림은 상태를 바꾸지 않는다")
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = AlertStatus.class, names = {"FIRED", "CANCELED"})
        void givenClosed_whenChanging_thenUnchanged(AlertStatus status) {
            AlertEntity entity = givenEntity(status);

            assertThat(sut.updateAlertStatusByFire(1L).previousStatus()).isEqualTo(status);
            assertThat(sut.updateAlertStatusByCancel(1L).previousStatus()).isEqualTo(status);
            assertThat(entity.getStatus()).isEqualTo(status);
        }

        @DisplayName("강제 취소: ACTIVE는 CANCELED로 바꾼다")
        @Test
        void givenActive_whenCanceling_thenCanceled() {
            AlertEntity entity = givenEntity(AlertStatus.ACTIVE);

            sut.updateAlertStatusByCancel(1L);

            assertThat(entity.getStatus()).isEqualTo(AlertStatus.CANCELED);
        }

        @DisplayName("사용자 취소: 소유자·종목이 다르거나 요청자가 없으면 empty, 일치하면 CANCELED")
        @Test
        void givenOwnership_whenUserCanceling_thenChecksOwner() {
            AlertEntity entity = givenEntity(AlertStatus.ACTIVE);

            assertThat(sut.updateAlertStatusByUserCancel(1L, "other", "005930")).isEmpty();
            assertThat(sut.updateAlertStatusByUserCancel(1L, "user", "000660")).isEmpty();
            assertThat(sut.updateAlertStatusByUserCancel(1L, null, "005930")).isEmpty();
            assertThat(entity.getStatus()).isEqualTo(AlertStatus.ACTIVE);

            assertThat(sut.updateAlertStatusByUserCancel(1L, "user", "005930")).isPresent();
            assertThat(entity.getStatus()).isEqualTo(AlertStatus.CANCELED);
        }

        @DisplayName("알림이 없으면 예외를 던진다")
        @Test
        void givenNotFound_whenChanging_thenThrows() {
            given(alertRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateAlertStatusByFire(1L)).hasMessage("alert not found");
        }

        @DisplayName("활성 알림 조회를 저장소에 위임한다")
        @Test
        void whenFindingActive_thenDelegates() {
            List<AlertEntity> entities = List.of(new AlertEntity());
            given(alertRepository.findAllActive(List.of("005930"))).willReturn(entities);

            assertThat(sut.findAllActiveAlerts(List.of("005930"))).isSameAs(entities);
        }
    }

    private void givenSaveAssignsId() {
        given(alertRepository.save(any(AlertEntity.class))).willAnswer(invocation -> {
            AlertEntity entity = invocation.getArgument(0);
            entity.setAlertId(1L);
            return entity;
        });
    }

    private AlertEntity givenEntity(AlertStatus status) {
        AlertEntity entity = AlertEntity.of("user", "005930", AlertDirection.ABOVE, 72_000, status);
        entity.setAlertId(1L);
        given(alertRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
        return entity;
    }
}
