package arile.toy.stocksystem.stockserver.otococancel.service;

import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.otococancel.dto.OtocoCancelErrorCode;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.otococancel.event.publisher.OtocoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otococancel.repository.OtocoCancelRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] OTOCO 취소 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoCancelServiceTest {

    @InjectMocks private OtocoCancelService sut;

    @Mock private OtocoRepository otocoRepository;
    @Mock private OtocoCancelRepository otocoCancelRepository;
    @Mock private OtocoEntryBookRegistry otocoEntryBookRegistry;
    @Mock private OtocoExitBookRegistry otocoExitBookRegistry;
    @Mock private OtocoCancelResponseEventPublisher otocoCancelResponseEventPublisher;
    @Mock private StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Mock private CancelService cancelService;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @Nested
    @DisplayName("registerCancel (사용자 취소)")
    class RegisterCancel {

        @DisplayName("소유자·종목이 다르거나 요청자가 없으면 아무것도 하지 않는다")
        @Test
        void givenNotOwner_whenCanceling_thenNothing() {
            OtocoEntity entity = givenEntity(OtocoStatus.WAITING_ENTRY, LeverageRatio.SPOT);

            sut.registerCancel(OtocoCancelRequestEvent.of(1L, "005930", "other"));
            sut.registerCancel(OtocoCancelRequestEvent.of(1L, "000660", "user"));
            sut.registerCancel(OtocoCancelRequestEvent.of(1L, "005930", null));

            assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.WAITING_ENTRY);
            then(otocoCancelResponseEventPublisher).shouldHaveNoInteractions();
            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("OTOCO가 없으면 예외를 던진다")
        @Test
        void givenNotFound_whenCanceling_thenThrows() {
            given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.registerCancel(request())).hasMessage("otoco not found");
        }

        @DisplayName("이미 취소·완료된 경우 각각의 에러 응답만 발행한다")
        @Test
        void givenClosed_whenCanceling_thenPublishesError() {
            OtocoEntity entity = OtocoFixtures.entity(OtocoStatus.CANCELED);
            OtocoEntity completed = OtocoFixtures.entity(OtocoStatus.COMPLETED);
            given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity), Optional.of(completed));

            sut.registerCancel(request());
            sut.registerCancel(request());

            ArgumentCaptor<OtocoCancelResponseEvent> captor = ArgumentCaptor.forClass(OtocoCancelResponseEvent.class);
            then(otocoCancelResponseEventPublisher).should(times(2)).publish(captor.capture());
            assertThat(captor.getAllValues()).extracting(OtocoCancelResponseEvent::errorCode)
                    .containsExactly(OtocoCancelErrorCode.ALREADY_CANCELLED, OtocoCancelErrorCode.ALREADY_COMPLETED);
            then(otocoCancelRepository).shouldHaveNoInteractions();
        }

        @DisplayName("진입 대기: 상태·이력 저장 → 예약액 환불 → 진입 북 제거 → 응답 삭제·성공 발행 순으로 처리한다")
        @Test
        void givenWaitingEntry_whenCanceling_thenSavesRefundsRemoves() {
            OtocoEntity entity = givenEntity(OtocoStatus.WAITING_ENTRY, LeverageRatio.X2);
            // 700,000 × 0.5 + 수수료 105
            given(accountApiClient.refundReservedCash("user", 350_105L)).willReturn(true);

            sut.registerCancel(request());

            InOrder inOrder = inOrder(otocoRepository, otocoCancelRepository, accountApiClient, otocoEntryBookRegistry,
                    stockServerOtocoResponseRepository, otocoCancelResponseEventPublisher);
            inOrder.verify(otocoRepository).save(entity);
            inOrder.verify(otocoCancelRepository).saveAndFlush(argThat(c -> c.getOtocoId().equals(1L)));
            inOrder.verify(accountApiClient).refundReservedCash("user", 350_105L);
            inOrder.verify(otocoEntryBookRegistry).remove("005930", 1L);
            inOrder.verify(stockServerOtocoResponseRepository).delete("user", 1L);
            inOrder.verify(otocoCancelResponseEventPublisher).publish(argThat(OtocoCancelResponseEvent::success));
            assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
        }

        @DisplayName("진입 주문 대기: 이력 저장 후 진입 주문을 강제 취소한다 (환불은 주문 취소가 담당)")
        @Test
        void givenEntryOrderPlaced_whenCanceling_thenForceCancelsOrder() {
            OtocoEntity entity = givenEntity(OtocoStatus.ENTRY_ORDER_PLACED, LeverageRatio.SPOT);
            entity.setEntryOrderId(100L);

            sut.registerCancel(request());

            InOrder inOrder = inOrder(otocoCancelRepository, cancelService);
            inOrder.verify(otocoCancelRepository).saveAndFlush(any());
            inOrder.verify(cancelService).forceCancel(100L);
            then(accountApiClient).shouldHaveNoInteractions();
            then(otocoCancelResponseEventPublisher).should().publish(argThat(OtocoCancelResponseEvent::success));
            assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
        }

        @DisplayName("청산 대기: 환불 없이 청산 북에서 제거한다 (청산 주식은 발동 시 예약)")
        @Test
        void givenWaitingExit_whenCanceling_thenRemovesWithoutRefund() {
            OtocoEntity entity = givenEntity(OtocoStatus.WAITING_EXIT, LeverageRatio.SPOT);

            sut.registerCancel(request());

            then(accountApiClient).shouldHaveNoInteractions();
            then(otocoExitBookRegistry).should().remove("005930", 1L);
            then(otocoCancelRepository).should().saveAndFlush(any());
            then(otocoCancelResponseEventPublisher).should().publish(argThat(OtocoCancelResponseEvent::success));
            assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
        }

        @DisplayName("이력 저장이 실패하면 환불하지 않고 에러 발행 후 예외를 다시 던진다 (롤백 후 재시도)")
        @Test
        void givenSaveFails_whenCanceling_thenNoRefundAndRethrows() {
            givenEntity(OtocoStatus.WAITING_ENTRY, LeverageRatio.SPOT);
            given(otocoCancelRepository.saveAndFlush(any())).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerCancel(request())).hasMessage("db error");

            then(accountApiClient).shouldHaveNoInteractions();
            then(otocoEntryBookRegistry).shouldHaveNoInteractions();
            then(otocoCancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == OtocoCancelErrorCode.INTERNAL_ERROR));
        }

        @DisplayName("환불이 실패하면 북에서 제거하지 않고 에러 발행 후 예외를 다시 던진다")
        @Test
        void givenRefundFails_whenCanceling_thenRethrows() {
            givenEntity(OtocoStatus.WAITING_ENTRY, LeverageRatio.SPOT);
            given(accountApiClient.refundReservedCash("user", 700_105L)).willReturn(false);

            assertThatThrownBy(() -> sut.registerCancel(request())).hasMessage("Cash refund failed");

            then(otocoEntryBookRegistry).shouldHaveNoInteractions();
            then(stockServerOtocoResponseRepository).shouldHaveNoInteractions();
            then(otocoCancelResponseEventPublisher).should().publish(argThat(e ->
                    e.errorCode() == OtocoCancelErrorCode.INTERNAL_ERROR));
        }

        @DisplayName("환불 이후 북 제거·응답 삭제가 실패해도 예외 없이 성공을 발행한다 (이중 환불 방지)")
        @Test
        void givenPostRefundFails_whenCanceling_thenStillSucceeds() {
            givenEntity(OtocoStatus.WAITING_ENTRY, LeverageRatio.SPOT);
            given(accountApiClient.refundReservedCash("user", 700_105L)).willReturn(true);
            willThrow(new IllegalStateException("book error")).given(otocoEntryBookRegistry).remove("005930", 1L);
            willThrow(new IllegalStateException("redis down")).given(stockServerOtocoResponseRepository).delete("user", 1L);

            sut.registerCancel(request());

            then(otocoCancelResponseEventPublisher).should().publish(argThat(OtocoCancelResponseEvent::success));
        }
    }

    @Nested
    @DisplayName("forceCancelOtoco (시스템 강제 취소)")
    class ForceCancel {

        @DisplayName("이미 종료된 OTOCO면 아무것도 하지 않는다")
        @Test
        void givenClosed_whenForceCanceling_thenNothing() {
            givenEntity(OtocoStatus.COMPLETED, LeverageRatio.SPOT);

            sut.forceCancelOtoco(1L);

            then(otocoCancelRepository).shouldHaveNoInteractions();
            then(otocoCancelResponseEventPublisher).shouldHaveNoInteractions();
        }

        @DisplayName("진입 주문 대기도 주문을 취소하고 응답 캐시 삭제·성공을 발행한다")
        @Test
        void givenEntryOrderPlaced_whenForceCanceling_thenCancelsAndPublishes() {
            OtocoEntity entity = givenEntity(OtocoStatus.ENTRY_ORDER_PLACED, LeverageRatio.SPOT);
            entity.setEntryOrderId(100L);

            sut.forceCancelOtoco(1L);

            then(cancelService).should().forceCancel(100L);
            then(stockServerOtocoResponseRepository).should().delete("user", 1L);
            then(otocoCancelResponseEventPublisher).should().publish(argThat(OtocoCancelResponseEvent::success));
        }

        @DisplayName("환불이 실패해도 예외를 던지지 않는다 (장 마감 정산에서 예약 해제)")
        @Test
        void givenRefundFails_whenForceCanceling_thenSwallows() {
            givenEntity(OtocoStatus.WAITING_ENTRY, LeverageRatio.SPOT);
            given(accountApiClient.refundReservedCash("user", 700_105L)).willReturn(false);

            sut.forceCancelOtoco(1L);

            then(otocoCancelResponseEventPublisher).shouldHaveNoInteractions();
        }
    }

    @DisplayName("강제 취소 시 OTOCO가 없으면 예외를 던진다")
    @Test
    void givenNotFound_whenForceCanceling_thenThrows() {
        given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.forceCancelOtoco(1L)).hasMessage("otoco not found");
    }

    @DisplayName("열린 상태가 아닌 OTOCO를 단계별 취소에 넘기면 예외를 던진다 (방어 코드)")
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OtocoStatus.class, names = {"COMPLETED", "CANCELED"})
    void givenClosedStatus_whenCancelOpen_thenThrows(OtocoStatus status) {
        OtocoEntity entity = OtocoFixtures.entity(1L, status, LeverageRatio.SPOT);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(sut, "cancelOpen", entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not an open otoco status: " + status);
    }

    private OtocoCancelRequestEvent request() {
        return OtocoCancelRequestEvent.of(1L, "005930", "user");
    }

    private OtocoEntity givenEntity(OtocoStatus status, LeverageRatio ratio) {
        OtocoEntity entity = OtocoFixtures.entity(1L, status, ratio);
        given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
        return entity;
    }
}
